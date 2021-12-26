package com.severtrans.notification.repository;

import java.util.Optional;

import com.severtrans.notification.model.Customer;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerDao extends CrudRepository<Customer,String>{
    /**
     * Поиск клиента по ВН
     * @param vn ВН клиента
     * @return Заказчик
     */
    Optional<Customer> findByClientId(@Param("vn") int vn);
}
