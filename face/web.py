import os
import cv2
import numpy as np
import base64
import time
import glob
from threading import Lock
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List
from PIL import Image

# ==================== 配置常量 ====================
DATA_PATH = "web_dataset"
TRAINER_FILE = "web_trainer.yml"
# 使用 OpenCV 默认的人脸级联分类器
HAAR_CASCADE_PATH = cv2.data.haarcascades + "haarcascade_frontalface_default.xml"

# 初始化目录
if not os.path.exists(DATA_PATH):
    os.makedirs(DATA_PATH)

# ==================== 全局 AI 模型 ====================
app = FastAPI(title="Face Recognition Microservice")

# 初始化人脸检测器和识别器
detector = cv2.CascadeClassifier(HAAR_CASCADE_PATH)
recognizer = cv2.face.LBPHFaceRecognizer_create()

# 线程锁：防止同时读写模型文件导致崩溃
model_lock = Lock()
model_trained = False


def load_model():
    global model_trained
    with model_lock:
        if os.path.exists(TRAINER_FILE):
            try:
                recognizer.read(TRAINER_FILE)
                model_trained = True
                print("模型加载成功！")
            except Exception as e:
                print(f"模型加载失败: {e}")
                model_trained = False
        else:
            model_trained = False
            print("当前无模型文件，等待录入人脸。")


# 启动时加载模型
load_model()


# ==================== 请求与响应实体 ====================
class RegisterRequest(BaseModel):
    user_id: int
    images_base64: List[str]  # 接收多张前端抓拍的图片用于训练


class RecognizeRequest(BaseModel):
    image_base64: str


class DeleteRequest(BaseModel):
    user_id: int


# ==================== 辅助函数 ====================
def base64_to_cv2(base64_str: str):
    """将前端传来的 Base64 字符串转换为 OpenCV 的 Numpy 格式图片"""
    try:
        # 去除前缀，例如 data:image/jpeg;base64,
        if "," in base64_str:
            base64_str = base64_str.split(",")[1]

        img_data = base64.b64decode(base64_str)
        nparr = np.frombuffer(img_data, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        return img
    except Exception as e:
        raise ValueError(f"图片解码失败: {str(e)}")


def train_model():
    """读取 dataset 文件夹，重新训练模型"""
    global model_trained
    image_paths = [os.path.join(DATA_PATH, f) for f in os.listdir(DATA_PATH) if f.endswith('.jpg')]

    if not image_paths:
        if os.path.exists(TRAINER_FILE):
            os.remove(TRAINER_FILE)
        model_trained = False
        return False

    faces = []
    ids = []

    for image_path in image_paths:
        try:
            filename = os.path.split(image_path)[-1]
            parts = filename.split(".")

            # 严格校验文件名格式：User.{ID}.{时间戳}.jpg
            if len(parts) >= 3 and parts[0] == "User":
                # 【关键限制】：OpenCV 的标签必须是整数(int)！
                user_id_int = int(parts[1])

                # 转换为灰度图
                PIL_img = Image.open(image_path).convert('L')
                img_numpy = np.array(PIL_img, 'uint8')

                # 只有在解析 user_id_int 没有报错的情况下，才同步追加到两个数组中
                faces.append(img_numpy)
                ids.append(user_id_int)
            else:
                print(f"跳过命名不规范的文件: {filename}")

        except Exception as e:
            print(f"处理文件 {image_path} 时发生异常: {e}")
            continue

    # 确保有成功匹配的数据
    if len(faces) > 0 and len(faces) == len(ids):
        print(f"开始训练模型，样本数: {len(faces)}，标签数: {len(ids)}")
        recognizer.train(faces, np.array(ids))
        recognizer.write(TRAINER_FILE)
        model_trained = True
        return True
    else:
        print("未提取到有效的训练数据！")
        return False


# ==================== API 接口 ====================

@app.post("/api/face/register")
async def register_face(req: RegisterRequest):
    """
    1. 录入人脸接口
    接收一批图片，提取人脸保存，并触发重新训练
    """
    if not req.images_base64:
        raise HTTPException(status_code=400, detail="未提供图片")

    saved_count = 0
    with model_lock:
        for b64 in req.images_base64:
            try:
                frame = base64_to_cv2(b64)
                gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
                faces = detector.detectMultiScale(gray, scaleFactor=1.2, minNeighbors=5)

                for (x, y, w, h) in faces:
                    # 裁剪人脸并保存，加上时间戳防止文件名重复
                    face_img = gray[y:y + h, x:x + w]
                    filename = f"User.{req.user_id}.{int(time.time() * 1000)}.jpg"
                    filepath = os.path.join(DATA_PATH, filename)
                    cv2.imwrite(filepath, face_img)
                    saved_count += 1
                    break  # 每张图只提取最大的一张脸
            except Exception as e:
                print(f"处理某张图片时出错: {e}")
                continue

        if saved_count > 0:
            # 录入成功后立刻重新训练模型
            train_model()
            return {"status": "success", "message": f"成功提取并保存了 {saved_count} 个人脸特征，模型已更新。"}
        else:
            raise HTTPException(status_code=400, detail="提供的所有图片中均未检测到清晰的人脸")


@app.post("/api/face/recognize")
async def recognize_face(req: RecognizeRequest):
    """
    2. 识别人脸接口 (用于登录)
    接收一张图片，返回匹配的 user_id
    """
    global model_trained

    with model_lock:
        if not model_trained:
            raise HTTPException(status_code=500, detail="模型未训练，系统中暂无任何人脸数据")

    try:
        frame = base64_to_cv2(req.image_base64)

        cv2.imwrite("debug_login.jpg", frame)

        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)

        # 提高 minNeighbors 使得检测更严格，减少误判
        faces = detector.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=4, minSize=(60, 60))

        if len(faces) == 0:
            raise HTTPException(status_code=400, detail="未检测到人脸，请正对摄像头并保证光线充足")

        # 找到画面中最大的人脸 (假设离镜头最近的是登录者)
        faces = sorted(faces, key=lambda f: f[2] * f[3], reverse=True)
        x, y, w, h = faces[0]

        with model_lock:
            user_id, confidence = recognizer.predict(gray[y:y + h, x:x + w])

        # confidence 越低越相似 (LBPH 算法特性)，通常 0 表示完全一致，<70 表示比较可靠
        # 你可以根据实际情况调整 70 这个阈值
        if confidence < 75:
            return {
                "status": "success",
                "user_id": user_id,
                "confidence": round(confidence, 2)
            }
        else:
            raise HTTPException(status_code=401, detail="面部信息不匹配")

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"识别过程发生内部错误: {str(e)}")


@app.post("/api/face/delete")
async def delete_face(req: DeleteRequest):
    """
    3. 删除人脸接口
    删除某个 user_id 的所有照片，并重新训练
    """
    with model_lock:
        # 匹配所有该 user_id 的图片
        pattern = os.path.join(DATA_PATH, f"User.{req.user_id}.*.jpg")
        files_to_delete = glob.glob(pattern)

        if not files_to_delete:
            return {"status": "success", "message": "该用户不存在人脸数据"}

        # 删除物理文件
        for f in files_to_delete:
            try:
                os.remove(f)
            except Exception as e:
                print(f"删除文件 {f} 失败: {e}")

        # 重新训练模型
        success = train_model()

    if success:
        return {"status": "success", "message": f"成功删除用户 {req.user_id} 的人脸数据，模型已更新"}
    else:
        return {"status": "success", "message": "已删除该用户。人脸库已清空。"}


# ==================== 启动服务 ==================== 
if __name__ == "__main__":
    import uvicorn

    # 运行在 8000 端口
    uvicorn.run(app, host="0.0.0.0", port=8000)
