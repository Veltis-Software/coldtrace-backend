package com.acme.coldtrace.platform.iam.infrastructure.persistence.jpa.adapters;

import com.acme.coldtrace.platform.iam.domain.model.aggregates.User;
import com.acme.coldtrace.platform.iam.domain.model.valueobjects.EmailAddress;
import com.acme.coldtrace.platform.iam.domain.repositories.UserRepository;
import com.acme.coldtrace.platform.iam.infrastructure.persistence.jpa.assemblers.UserPersistenceAssembler;
import com.acme.coldtrace.platform.iam.infrastructure.persistence.jpa.repositories.UserPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA-backed adapter for the user domain repository.
 *
 * @since 1.0
 */
@Repository
public class UserRepositoryImpl implements UserRepository {
    private final UserPersistenceRepository userPersistenceRepository;

    public UserRepositoryImpl(UserPersistenceRepository userPersistenceRepository) {
        this.userPersistenceRepository = userPersistenceRepository;
    }

    @Override
    public List<User> findAllByOrganizationId(Long organizationId) {
        return userPersistenceRepository.findAllByOrganizationId(organizationId).stream()
                .map(UserPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public Optional<User> findByIdAndOrganizationId(Long id, Long organizationId) {
        return userPersistenceRepository.findByIdAndOrganizationId(id, organizationId)
                .map(UserPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userPersistenceRepository.findById(id)
                .map(UserPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userPersistenceRepository.findByEmail(new EmailAddress(email))
                .map(UserPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            var entity = UserPersistenceAssembler.toPersistenceFromDomain(user);
            return UserPersistenceAssembler.toDomainFromPersistence(userPersistenceRepository.save(entity));
        }

        var entity = userPersistenceRepository.findById(user.getId())
                .orElseGet(() -> UserPersistenceAssembler.toPersistenceFromDomain(user));
        UserPersistenceAssembler.copyDomainState(user, entity);
        return UserPersistenceAssembler.toDomainFromPersistence(userPersistenceRepository.save(entity));
    }

    @Override
    public boolean existsByEmail(String email) {
        return userPersistenceRepository.existsByEmail(new EmailAddress(email));
    }

    @Override
    public void deleteById(Long id) {
        userPersistenceRepository.deleteById(id);
        userPersistenceRepository.flush();
    }
}
