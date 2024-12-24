package AWS.API.entity;


import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "_aws_s3_bucket")
public class S3Bucket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;


    private String bucketIdentifier;

    // Many-to-one relationship with User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
