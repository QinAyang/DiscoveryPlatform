package com.nepxion.discovery.platform.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nepxion.discovery.platform.server.ui.entity.dto.SysPage;
import com.nepxion.discovery.platform.server.ui.entity.vo.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>Title: Nepxion Discovery</p>
 * <p>Description: Nepxion Discovery</p>
 * <p>Copyright: Copyright (c) 2017-2050</p>
 * <p>Company: Nepxion</p>
 *
 * @author Ning Zhang
 * @version 1.0
 */

@Mapper
public interface DbPageMapper extends BaseMapper<SysPage> {

    IPage<Page> list(IPage<Page> result,
                     @Param("name") String name);

    Long getMaxOrderNum(@Param("parentId") Long parentId);

    List<Page> listPermissionPages(@Param("adminId") Long adminId);
}