package glorydark.wardensystem.data;

import lombok.Data;

@Data
public class StyleData {
    private String ban = "您已被封禁";
    private String kick = "您被踢出服务器";
    private String mute = "您已被禁言";
    private String warn = "请规范您的游戏行为";
    private String rp = "有新的举报信息";
}
