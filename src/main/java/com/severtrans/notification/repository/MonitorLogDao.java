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
    //ORDER_UID,STATUS,MSG_TYPE,FILE_NAME,START_DATE,END_DATE,MSG,VN,INFO,ID
    @Query("SELECT * FROM MONITOR_LOG WHERE END_DATE IS NULL ORDER BY VN, MSG_TYPE DESC")
    List<MonitorLog> findAllIncompleted();

    @Modifying
    @Query("UPDATE monitor_log SET end_date = :ed WHERE id = :id")
    boolean completeOrder(@Param("id") Long id, @Param("ed") Date ed);

    
}
