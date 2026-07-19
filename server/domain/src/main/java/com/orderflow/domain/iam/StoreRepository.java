package com.orderflow.domain.iam;

import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface StoreRepository extends Repository<Store, Long> {

    Store save(Store store);

    Optional<Store> findById(Long id);
}
