package com.trinity.banking.infrastructure.adapter.out.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.trinity.banking.infrastructure.adapter.out.persistence.entity.TransactionEntity;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, Long> {

    @Query("""
            select t from TransactionEntity t
            where t.accountId = :accountId
              and t.createdAt >= :from
              and t.createdAt < :toExclusive
            order by t.createdAt asc, t.id asc
            """)
    List<TransactionEntity> findStatement(@Param("accountId") Long accountId,
                                          @Param("from") LocalDateTime from,
                                          @Param("toExclusive") LocalDateTime toExclusive);
}
