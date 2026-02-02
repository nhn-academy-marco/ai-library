package com.nhnacademy.library.core.book.repository;

import com.nhnacademy.library.core.book.domain.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book,Long>,BookRepositoryCustom{
}
