// mapper/SatelliteDataSampledMapper.java
package com.tzy.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tzy.sky.entity.SatelliteDataSampled;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SatelliteDataSampledMapper extends BaseMapper<SatelliteDataSampled> {

    /**
     * 批量插入
     */
    int insertBatch(@Param("list") List<SatelliteDataSampled> list);

    /**
     * 查询时间范围
     */
    @Select("SELECT MIN(timestamp) as startTime, MAX(timestamp) as endTime " +
            "FROM satellite_data_sampled WHERE satellite_id = #{satelliteId}")
    Map<String, Object> selectTimeRange(@Param("satelliteId") String satelliteId);
}