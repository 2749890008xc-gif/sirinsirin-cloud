package com.sirinsirin.entity.enums;


public enum EmailCheckCodeTypeEnum {
    register(0, "注册"),
    update_password(1, "修改密码");

    private Integer type;
    private String desc;

    EmailCheckCodeTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static EmailCheckCodeTypeEnum getByType(Integer type) {
        for (EmailCheckCodeTypeEnum item : EmailCheckCodeTypeEnum.values()) {
            if (item.getType().equals(type)) {
                return item;
            }
        }
        return null;
    }

    public Integer getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }
}
