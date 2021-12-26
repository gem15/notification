package com.severtrans.notification.repository;
import java.util.Date;
import java.util.List;

import com.severtrans.notification.model.MonitorLog;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MonitorLogDao extends CrudRepository<MonitorLog,Long>{
    
    @Query("SELECT * FROM monitor_log WHERE end_date IS NULL ORDER BY vn, msg_type DESC")
    List<MonitorLog> findIncompleted();

    @Modifying
    @Query("UPDATE monitor_log SET end_date = :ed WHERE id = :id")
    boolean completeOrder(@Param("id") Long id, @Param("ed") Date ed);

    
}
