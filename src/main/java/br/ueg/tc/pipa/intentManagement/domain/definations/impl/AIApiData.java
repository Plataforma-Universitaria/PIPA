package br.ueg.tc.pipa.intentManagement.domain.definations.impl;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.SEQUENCE;

@Entity
@Table(name = "ai_api")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AIApiData {

    @SequenceGenerator(
            name = "ai_api_generator_sequence",
            sequenceName = "ai_api_sequence",
            allocationSize = 1
    )

    @GeneratedValue(
            strategy = SEQUENCE,
            generator = "ai_api_generator_sequence"
    )

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "short_name", length = 10, nullable = false, unique = true)
    private String shortName;

    @Column(name = "url_api", length = 300, nullable = false, unique = true)
    private String urlApi;

    @Column(name = "params_api", length = 400, nullable = false)
    private String paramsApi;

    @Column(name = "classpath_api", length = 200, nullable = false)
    private String classpathApi;

    @Column(name = "key_envpath_api", length = 100, nullable = false)
    private String keyEnvPath;

    @Column(name = "active", nullable = false)
    private Boolean active;
}
