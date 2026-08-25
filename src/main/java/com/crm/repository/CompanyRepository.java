package com.crm.repository;

import com.crm.entity.Company;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 企业表仓储。
 */
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByCompanyNo(String companyNo);

    boolean existsByCompanyNo(String companyNo);

    /** 我拥有的企业 */
    List<Company> findByOwnerUserId(Long ownerUserId);
}
