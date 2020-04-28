package com.gmsj.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @author baojieren
 * @date 2020/4/17 14:12
 */
@Data
public class AuthPathBo implements Serializable {
    public String path;
    public String roles;
    public String actions;
}
