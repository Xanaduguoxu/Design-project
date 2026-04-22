import os
import sqlite3
import tkinter as tk
from datetime import datetime
from tkinter import ttk, messagebox, filedialog

import cv2
import numpy as np
import pandas as pd
from PIL import Image, ImageTk

DB_NAME = "attendance_system.db"
DATA_PATH = "dataset"
TRAINER_FILE = "trainer.yml"
HAAR_CASCADE_PATH = cv2.data.haarcascades + "haarcascade_frontalface_default.xml"

if not os.path.exists(DATA_PATH):
    os.makedirs(DATA_PATH)


# 数据库操作类
class DBManager:
    def __init__(self):
        self.conn = sqlite3.connect(DB_NAME)
        self.cursor = self.conn.cursor()
        self.create_tables()

    def create_tables(self):
        # 系统管理员表 (用于登录)
        self.cursor.execute('''
            CREATE TABLE IF NOT EXISTS admins (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL
            )
        ''')
        # 考勤用户表 (被录入的学生/员工)
        self.cursor.execute('''
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL
            )
        ''')
        # 考勤记录表
        self.cursor.execute('''
            CREATE TABLE IF NOT EXISTS attendance (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                user_name TEXT,
                type TEXT,
                timestamp TEXT,
                date TEXT
            )
        ''')
        self.conn.commit()

    def register_admin(self, username, password):
        try:
            self.cursor.execute("INSERT INTO admins (username, password) VALUES (?, ?)", (username, password))
            self.conn.commit()
            return True
        except sqlite3.IntegrityError:
            return False

    def check_admin(self, username, password):
        self.cursor.execute("SELECT * FROM admins WHERE username=? AND password=?", (username, password))
        return self.cursor.fetchone() is not None

    def add_user(self, name):
        self.cursor.execute("INSERT INTO users (name) VALUES (?)", (name,))
        self.conn.commit()
        return self.cursor.lastrowid

    def get_user_name(self, user_id):
        self.cursor.execute("SELECT name FROM users WHERE id=?", (user_id,))
        result = self.cursor.fetchone()
        return result[0] if result else "Unknown"

    def get_all_users(self):
        self.cursor.execute("SELECT * FROM users ORDER BY id DESC")
        return self.cursor.fetchall()

    def log_attendance(self, user_id, type_):
        name = self.get_user_name(user_id)
        now = datetime.now()
        timestamp = now.strftime("%H:%M:%S")
        date_today = now.strftime("%Y-%m-%d")

        # 防抖逻辑
        self.cursor.execute('''
            SELECT * FROM attendance 
            WHERE user_id=? AND type=? AND date=? 
            ORDER BY id DESC LIMIT 1
        ''', (user_id, type_, date_today))
        last_record = self.cursor.fetchone()

        if last_record:
            last_time_str = last_record[4]
            last_time = datetime.strptime(last_time_str, "%H:%M:%S")
            last_time_full = last_time.replace(year=now.year, month=now.month, day=now.day)

            seconds_diff = (now - last_time_full).seconds

            # 如果距离上次同类型打卡不足 5 秒，则视为重复，不记录
            if seconds_diff < 5:
                return False, f"刚刚已{type_}"

        # 写入新记录
        self.cursor.execute("INSERT INTO attendance (user_id, user_name, type, timestamp, date) VALUES (?, ?, ?, ?, ?)",
                            (user_id, name, type_, timestamp, date_today))
        self.conn.commit()
        return True, f"{type_}成功"

    def get_logs(self):
        self.cursor.execute("SELECT * FROM attendance ORDER BY id DESC")
        return self.cursor.fetchall()

    def close(self):
        self.conn.close()


# 登录页面
class LoginWindow:
    def __init__(self, root, db, on_login_success):
        self.root = root
        self.db = db
        self.on_login_success = on_login_success

        self.root.title("系统登录")
        self.root.geometry("400x300")

        # 居中显示
        screen_width = root.winfo_screenwidth()
        screen_height = root.winfo_screenheight()
        x = (screen_width - 400) // 2
        y = (screen_height - 300) // 2
        self.root.geometry(f"400x300+{x}+{y}")

        frame = tk.Frame(self.root)
        frame.pack(expand=True)

        tk.Label(frame, text="考勤系统登录", font=("Helvetica", 20, "bold")).pack(pady=20)

        tk.Label(frame, text="账号:").pack()
        self.entry_user = tk.Entry(frame)
        self.entry_user.pack(pady=5)

        tk.Label(frame, text="密码:").pack()
        self.entry_pass = tk.Entry(frame, show="*")
        self.entry_pass.pack(pady=5)

        tk.Button(frame, text="登录", command=self.login, width=15, bg="#4CAF50", fg="white").pack(pady=15)
        tk.Button(frame, text="注册管理员", command=self.register, width=15).pack()

    def login(self):
        u = self.entry_user.get()
        p = self.entry_pass.get()
        if self.db.check_admin(u, p):
            self.root.destroy()
            self.on_login_success()
        else:
            messagebox.showerror("错误", "账号或密码错误")

    def register(self):
        u = self.entry_user.get()
        p = self.entry_pass.get()
        if not u or not p:
            messagebox.showwarning("警告", "账号或密码不能为空")
            return
        if self.db.register_admin(u, p):
            messagebox.showinfo("成功", "注册成功，请登录")
        else:
            messagebox.showerror("错误", "账号已存在")


# 主应用程序
class AttendanceApp:
    def __init__(self):
        self.db = DBManager()

        # 首先显示登录窗口
        self.login_root = tk.Tk()
        LoginWindow(self.login_root, self.db, self.show_main_app)
        self.login_root.mainloop()

    def show_main_app(self):
        # 登录成功后，创建主窗口
        self.root = tk.Tk()
        self.root.title("人脸识别考勤系统")
        self.root.geometry("900x650")

        self.detector = cv2.CascadeClassifier(HAAR_CASCADE_PATH)
        self.recognizer = cv2.face.LBPHFaceRecognizer_create()

        if os.path.exists(TRAINER_FILE):
            self.recognizer.read(TRAINER_FILE)
            self.model_trained = True
        else:
            self.model_trained = False

        self.setup_ui()
        self.root.mainloop()

    def setup_ui(self):
        title_lbl = tk.Label(self.root, text="智慧课堂考勤系统", font=("Helvetica", 24, "bold"), bg="#3b5998",
                             fg="white")
        title_lbl.pack(fill=tk.X, pady=0)

        main_frame = tk.Frame(self.root)
        main_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)

        left_frame = tk.Frame(main_frame, width=220, bg="#f0f0f0")
        left_frame.pack(side=tk.LEFT, fill=tk.Y, padx=5)

        right_frame = tk.Frame(main_frame, bg="white")
        right_frame.pack(side=tk.RIGHT, fill=tk.BOTH, expand=True, padx=5)

        btn_style = {"font": ("Arial", 12), "width": 22, "height": 2, "relief": tk.GROOVE}

        tk.Button(left_frame, text="1. 新用户注册 (采集)", command=self.register_user_window, bg="#607D8B", fg="white",
                  **btn_style).pack(pady=10)
        tk.Button(left_frame, text="2. 训练人脸模型", command=self.train_model, bg="#FF9800", fg="white",
                  **btn_style).pack(pady=10)
        tk.Frame(left_frame, height=2, bg="grey", width=200).pack(pady=10)

        tk.Button(left_frame, text="3. 自动签到 (Sign In)", command=lambda: self.attendance_window("签到"),
                  bg="#4CAF50", fg="white", **btn_style).pack(pady=5)
        tk.Button(left_frame, text="4. 自动签退 (Sign Out)", command=lambda: self.attendance_window("签退"),
                  bg="#2196F3", fg="white", **btn_style).pack(pady=5)

        tk.Frame(left_frame, height=2, bg="grey", width=200).pack(pady=10)

        tk.Button(left_frame, text="5. 人员信息列表", command=self.users_list_window, bg="#00BCD4", fg="white",
                  **btn_style).pack(pady=10)

        tk.Button(left_frame, text="6. 查看/导出 报表", command=self.report_window, bg="#9C27B0", fg="white",
                  **btn_style).pack(pady=10)
        tk.Button(left_frame, text="退出登录", command=self.logout, bg="#f44336", fg="white", **btn_style).pack(
            pady=20)

        self.status_label = tk.Label(right_frame, text="欢迎使用\n请点击左侧按钮开始", font=("Arial", 16))
        self.status_label.pack(expand=True)

    def logout(self):
        self.root.destroy()
        # 重启程序到登录界面
        app = AttendanceApp()

    def init_camera(self):
        cap = cv2.VideoCapture(0)
        cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
        return cap

    # 1. 用户注册
    def register_user_window(self):
        win = tk.Toplevel(self.root)
        win.title("用户注册")
        win.geometry("400x200")

        tk.Label(win, text="请输入姓名:").pack(pady=10)
        entry_name = tk.Entry(win, font=("Arial", 14))
        entry_name.pack(pady=5)

        def start_capture():
            name = entry_name.get()
            if not name:
                messagebox.showerror("错误", "姓名不能为空")
                return

            user_id = self.db.add_user(name)
            win.destroy()
            self.capture_faces(user_id, name)

        tk.Button(win, text="开始采集人脸 (需开启摄像头)", command=start_capture, bg="#4CAF50", fg="white").pack(
            pady=20)

    def capture_faces(self, user_id, name):
        cam = self.init_camera()
        count = 0
        total_pics = 50

        while True:
            ret, frame = cam.read()
            if not ret: break

            frame = cv2.flip(frame, 1)  # 镜像
            gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            faces = self.detector.detectMultiScale(gray, 1.3, 5)

            for (x, y, w, h) in faces:
                cv2.rectangle(frame, (x, y), (x + w, y + h), (255, 0, 0), 2)
                count += 1
                cv2.imwrite(f"{DATA_PATH}/User.{user_id}.{count}.jpg", gray[y:y + h, x:x + w])
                cv2.putText(frame, f"Capturing {count}/{total_pics}", (x, y - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.6,
                            (0, 255, 0), 1)

            cv2.imshow('Face Capture', frame)

            k = cv2.waitKey(100) & 0xff
            if k == 27 or count >= total_pics:
                break

        cam.release()
        cv2.destroyAllWindows()
        messagebox.showinfo("成功", f"用户 {name} 采集完成！请点击'训练模型'。")

    # 2. 训练模型
    def train_model(self):
        image_paths = [os.path.join(DATA_PATH, f) for f in os.listdir(DATA_PATH) if f.endswith('.jpg')]
        if not image_paths:
            messagebox.showwarning("警告", "没有找到数据，请先注册用户。")
            return

        faces = []
        ids = []

        for image_path in image_paths:
            try:
                PIL_img = Image.open(image_path).convert('L')
                img_numpy = np.array(PIL_img, 'uint8')
                id = int(os.path.split(image_path)[-1].split(".")[1])
                faces.append(img_numpy)
                ids.append(id)
            except:
                continue

        self.recognizer.train(faces, np.array(ids))
        self.recognizer.write(TRAINER_FILE)
        self.model_trained = True
        messagebox.showinfo("成功", f"模型训练完成！\nID数量: {len(np.unique(ids))}")

    # 3. 自动考勤
    def attendance_window(self, mode):
        if not self.model_trained:
            messagebox.showerror("错误", "请先训练模型！")
            return

        win = tk.Toplevel(self.root)
        win.title(f"自动{mode}中... (成功后定格并退出)")
        win.geometry("800x650")

        tk.Label(win, text=f"当前模式: {mode}", font=("Arial", 18, "bold"), fg="red").pack(pady=10)

        video_frame = tk.Frame(win, bg="black", width=640, height=480)
        video_frame.pack(pady=5)
        video_label = tk.Label(video_frame)
        video_label.pack()

        info_label = tk.Label(win, text="请正对摄像头，识别成功后画面会定格并自动关闭", font=("Arial", 12))
        info_label.pack(pady=10)

        self.cap = self.init_camera()
        self.is_recognizing = True

        def close_window():
            self.is_recognizing = False
            self.cap.release()
            win.destroy()

        tk.Button(win, text="取消/关闭", command=close_window, bg="#f44336", fg="white", font=("Arial", 14),
                  width=20).pack(pady=5)

        def process_frame():
            if not self.is_recognizing: return

            ret, frame = self.cap.read()
            if ret:
                frame = cv2.flip(frame, 1)  # 镜像
                gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
                faces = self.detector.detectMultiScale(gray, 1.2, 5)

                for (x, y, w, h) in faces:
                    color = (0, 0, 255)  # 默认红色

                    id, confidence = self.recognizer.predict(gray[y:y + h, x:x + w])

                    if confidence < 70:
                        name = self.db.get_user_name(id)
                        is_new_record, msg = self.db.log_attendance(id, mode)

                        if is_new_record:
                            # 1. 成功且是新记录 (或者超过了5秒冷却期)
                            color = (0, 255, 0)
                            cv2.rectangle(frame, (x, y), (x + w, y + h), color, 3)
                            cv2.putText(frame, f"{name} Success!", (x + 5, y - 10), cv2.FONT_HERSHEY_SIMPLEX, 1, color,
                                        2)

                            # 大字提示
                            cv2.putText(frame, "Done! Closing...", (20, 450), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0),
                                        2)

                            img = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                            img = Image.fromarray(img)
                            imgtk = ImageTk.PhotoImage(image=img)
                            video_label.imgtk = imgtk
                            video_label.configure(image=imgtk)

                            self.is_recognizing = False
                            win.after(2000, close_window)
                            return
                        else:
                            # 5秒内重复打卡
                            color = (0, 255, 255)
                            cv2.putText(frame, f"{name} Checked", (x + 5, y - 10), cv2.FONT_HERSHEY_SIMPLEX, 1, color,
                                        2)
                            cv2.rectangle(frame, (x, y), (x + w, y + h), color, 2)

                    else:
                        name = "Unknown"
                        cv2.putText(frame, name, (x + 5, y - 10), cv2.FONT_HERSHEY_SIMPLEX, 1, color, 2)
                        cv2.rectangle(frame, (x, y), (x + w, y + h), color, 2)

                img = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                img = Image.fromarray(img)
                imgtk = ImageTk.PhotoImage(image=img)
                video_label.imgtk = imgtk
                video_label.configure(image=imgtk)

            win.after(10, process_frame)

        process_frame()

    # 4. 报表
    def report_window(self):
        win = tk.Toplevel(self.root)
        win.title("考勤数据报表")
        win.geometry("900x500")

        columns = ("ID", "用户ID", "姓名", "类型", "时间", "日期")
        tree = ttk.Treeview(win, columns=columns, show="headings")

        for col in columns:
            tree.heading(col, text=col)
            tree.column(col, width=120, anchor="center")

        tree.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)

        records = self.db.get_logs()
        for row in records:
            tree.insert("", tk.END, values=row)

        def export_csv():
            if not records:
                messagebox.showwarning("空", "没有数据可导出")
                return
            file_path = filedialog.asksaveasfilename(defaultextension=".csv", filetypes=[("CSV files", "*.csv")])
            if file_path:
                df = pd.DataFrame(records, columns=["RecordID", "UserID", "Name", "Type", "Time", "Date"])
                df.to_csv(file_path, index=False, encoding='utf_8_sig')
                messagebox.showinfo("成功", f"导出成功: {file_path}")

        tk.Button(win, text="导出 CSV", command=export_csv, bg="#2196F3", fg="white", height=2, width=20).pack(pady=10)

    # 5. 人员信息列表
    def users_list_window(self):
        win = tk.Toplevel(self.root)
        win.title("已录入人员信息")
        win.geometry("600x400")

        columns = ("ID", "姓名")
        tree = ttk.Treeview(win, columns=columns, show="headings")

        tree.heading("ID", text="用户 ID")
        tree.column("ID", width=150, anchor="center")
        tree.heading("姓名", text="姓名")
        tree.column("姓名", width=300, anchor="center")

        tree.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)

        users = self.db.get_all_users()
        if not users:
            tree.insert("", tk.END, values=("暂无数据", "请先注册用户"))
        else:
            for row in users:
                tree.insert("", tk.END, values=row)


if __name__ == "__main__":
    app = AttendanceApp()
