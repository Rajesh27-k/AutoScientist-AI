package com.autoscientist.repository;

import com.autoscientist.entity.EvidenceNode;
import com.autoscientist.entity.Finding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvidenceNodeRepository extends JpaRepository<EvidenceNode, Long> {
    List<EvidenceNode> findByFindingOrderByCreatedAtAsc(Finding finding);
}
