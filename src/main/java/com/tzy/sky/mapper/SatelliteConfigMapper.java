// mapper/SatelliteConfigMapper.java
package com.tzy.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tzy.sky.entity.SatelliteConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 卫星配置 Mapper 接口
 * 使用 MyBatis-Plus 进行数据库操作
 */
@Mapper
public interface SatelliteConfigMapper extends BaseMapper<SatelliteConfig> {

    /**
     * 查询所有启用的卫星配置
     * @return 启用的卫星配置列表
     */
    @Select("SELECT * FROM satellite_config WHERE enabled = true ORDER BY id")
    List<SatelliteConfig> selectAllEnabled();

    /**
     * 根据ID查询卫星配置
     * @param id 卫星ID
     * @return 卫星配置
     */
    @Select("SELECT * FROM satellite_config WHERE id = #{id}")
    SatelliteConfig selectBySatelliteId(@Param("id") String id);
}
