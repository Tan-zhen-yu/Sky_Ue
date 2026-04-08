// mapper/SatelliteDataMapper.java
package com.tzy.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tzy.sky.entity.SatelliteData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SatelliteDataMapper extends BaseMapper<SatelliteData> {
    /**
     * 批量插入（MySQL）
     */
    int insertBatch(@Param("list") List<SatelliteData> list);
}