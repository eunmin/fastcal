package com.fastcal.domain.repository;

import com.fastcal.domain.model.User;
import com.fastcal.domain.model.vo.Email;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<User, Long> {

  Mono<User> findByEmail(Email email);
}
