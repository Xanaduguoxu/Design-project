package org.example.pojo.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.example.pojo.base.Base;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class User extends Base {

    private String username;

    private String password;

    private String role;

    private String nickname;

    private String avatar;

    private String gender;

    private String email;

    private String birthday;
}
