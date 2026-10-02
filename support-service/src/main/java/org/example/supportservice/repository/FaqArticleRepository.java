package org.example.supportservice.repository;

import org.example.supportservice.model.FaqArticle;
import org.example.supportservice.model.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqArticleRepository extends JpaRepository<FaqArticle, Long> {

    List<FaqArticle> findByCategory(TicketCategory category);
}
