package AWS.API.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "_roles_aws")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long Id;

    @NotNull
    @Column(unique = true)
    private String name;

}
