/*
 *@Type RespDTO.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 13:40
 * @version
 */
package dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class RespDTO implements Serializable {
    private RespStatusTypeEnum status;
    private String value;

    public RespDTO(RespStatusTypeEnum status, String value) {
        this.status = status;
        this.value = value;
    }

    @Override
    public String toString() {
        return "RespDTO{" +
                "status=" + status +
                ", value='" + value + '\'' +
                '}';
    }
}
