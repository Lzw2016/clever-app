package org.clever.app.mvc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/03/21 21:46 <br/>
 */
@Getter
@Setter
public class Entity1 {
    @JsonProperty("name_06")
    private String name;
    private int age;
}
