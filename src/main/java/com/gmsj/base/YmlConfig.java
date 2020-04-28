package com.gmsj.base;

import com.gmsj.model.AuthPathBo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author baojieren
 * @date 2020/4/17 13:17
 */
@Data
@Component
@ConfigurationProperties(prefix = "appconfig")
public class YmlConfig {

    /**
     * 不需要鉴权的接口
     */
    public List<String> anonPathList;

    /**
     * 需要授权的接口
     */
    public List<AuthPathBo> authPathList;

    /**
     * 服务地址
     */
    public String authUrl;
    public String miniUrl;
    public String mngUrl;
}
