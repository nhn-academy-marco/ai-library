package net.java21.library.core.book.domain;

import net.java21.library.core.review.domain.BookReviewSummary;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "books",
        indexes = {
            @Index(name = "idx_book_isbn", columnList = "isbn", unique = true)
        }
)

@Getter
@NoArgsConstructor
@ToString(exclude = "reviewSummary")
public class Book {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "book_sequence_generator"
    )

    @SequenceGenerator(
            name = "book_sequence_generator",
            sequenceName = "public.book_sequence",
            allocationSize = 1000
    )
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id", referencedColumnName = "book_id", insertable = false, updatable = false)
    private BookReviewSummary reviewSummary;

    @Column(name = "isbn", length = 20, unique = true, nullable = false)
    private String isbn;

    @Column(name = "volume_title", length = 255)
    private String volumeTitle;

    @Column(name = "title", length = 500, nullable = false)
    private String title;

    @Column(name = "author_name", length = 1000)
    private String authorName;

    @Column(name = "publisher_name", length = 255)
    private String publisherName;

    @Column(name = "first_publish_date")
    private LocalDate firstPublishDate;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "book_content", columnDefinition = "TEXT")
    private String bookContent;

    @Column(name = "subtitle", length = 500)
    private String subtitle;

    @Column(name = "edition_publish_date")
    private LocalDate editionPublishDate;

    @Column(name="created_at",updatable = false,nullable = false)
    private OffsetDateTime createdAt;

    @Column(name="updated_at")
    private OffsetDateTime updatedAt;

    @Convert(converter = VectorConverter.class)
    @Column(name = "embedding", columnDefinition = "vector(1024)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private float[] embedding;

    public void updateEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    /**
     * 표지 이미지가 있는지 확인합니다.
     *
     * @return 표지 이미지 URL이 비어 있지 않으면 true
     */
    public boolean hasCoverImage() {
        return imageUrl != null && !imageUrl.isBlank();
    }

    /**
     * 표지 이미지가 비어 있을 때만 외부에서 찾은 표지 URL로 보강합니다.
     *
     * <p>이미 표지가 있으면 덮어쓰지 않습니다(CSV 원본 데이터 우선).</p>
     *
     * @param coverImageUrl 보강할 표지 이미지 URL
     * @return 실제로 변경했으면 true
     */
    public boolean fillMissingCoverImage(String coverImageUrl) {
        if (hasCoverImage() || coverImageUrl == null || coverImageUrl.isBlank()) {
            return false;
        }
        this.imageUrl = coverImageUrl;
        return true;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Book(
            String isbn,
            String volumeTitle,
            String title,
            String authorName,
            String publisherName,
            LocalDate firstPublishDate,
            BigDecimal price,
            String imageUrl,
            String bookContent,
            String subtitle,
            LocalDate editionPublishDate
    ) {
        this.isbn = isbn;
        this.volumeTitle = volumeTitle;
        this.title = title;
        this.authorName = authorName;
        this.publisherName = publisherName;
        this.firstPublishDate = firstPublishDate;
        this.price = price;
        this.imageUrl = imageUrl;
        this.bookContent = bookContent;
        this.subtitle = subtitle;
        this.editionPublishDate = editionPublishDate;
    }

}
