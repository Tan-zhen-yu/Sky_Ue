// mapper/GroundStationConfigMapper.java
package com.tzy.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tzy.sky.entity.GroundStationConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 地面站配置 Mapper 接口
 * 使用 MyBatis-Plus 进行数据库操作
 */
@Mapper
public interface GroundStationConfigMapper extends BaseMapper<GroundStationConfig> {

    /**
     * 查询所有启用的地面站配置
     * @return 启用的地面站配置列表
     */
    @Select("SELECT * FROM ground_station_config WHERE enabled = true ORDER BY id")
    List<GroundStationConfig> selectAllEnabled();

    /**
     * 根据ID查询地面站配置
     * @param id 地面站ID
     * @return 地面站配置
     */
    @Select("SELECT * FROM ground_station_config WHERE id = #{id}")
    GroundStationConfig selectByStationId(@Param("id") String id);
}
