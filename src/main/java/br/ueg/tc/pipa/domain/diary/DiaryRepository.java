package br.ueg.tc.pipa.domain.diary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {

    Optional<Diary> getDiaryByDateAndUser_Id(LocalDateTime date, Long userId);
    Optional<List<Diary>> getDiariesByUser_Id(Long userId);

}
