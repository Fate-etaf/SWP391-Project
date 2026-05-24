package com.swp5.library_management.repository;

import com.swp5.library_management.Entity.Book;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {

    /**
     * Lấy 4 sách mới nhập kho nhất, sắp xếp theo {@code createdAt} giảm dần.
     *
     * <p>Annotation {@code @EntityGraph} yêu cầu JPA tải eagerly các quan hệ
     * {@code authors}, {@code categories}, {@code copies}, và {@code subject}
     * trong cùng 1 câu SQL (dùng JOIN), thay vì lazy-load riêng lẻ từng cái.
     * Điều này giúp tránh hoàn toàn lỗi "LazyInitializationException" và
     * bài toán N+1 queries khi Service truy cập các collection này.
     */
    @EntityGraph(attributePaths = {"authors", "categories", "copies", "subject"})
    List<Book> findTop4ByOrderByCreatedAtDesc();

    /**
     * Đếm số sách theo từng thể loại bằng 1 câu query duy nhất.
     *
     * <p>Trả về List<Object[]> với mỗi phần tử là [CategoryID (Integer), count (Long)].
     * Dùng trong Service để build một Map tra cứu nhanh, tránh việc gọi DB
     * N lần (một lần cho mỗi category).
     *
     * <p>Dùng {@code COUNT(DISTINCT b)} để đảm bảo mỗi cuốn sách chỉ được
     * đếm 1 lần dù nó có nhiều bản sao.
     */
    @Query("SELECT c.categoryId, COUNT(DISTINCT b) FROM Book b JOIN b.categories c GROUP BY c.categoryId")
    List<Object[]> countBooksGroupedByCategory();
}
