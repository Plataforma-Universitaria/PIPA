package br.ueg.tc.pipa.domain.diary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {

    Optional<Diary> getDiaryByDateAndUser_Id(LocalDateTime date, Long userId);
    Optional<List<Diary>> getDiariesByUser_Id(Long userId);
    Optional<Diary> findByDateAndUser_Id(LocalDateTime date, Long userId);

    @Modifying
    @Query("delete from Diary d where d.date = :date and d.user.id = :userId")
    void deleteByDateAndUserId(@Param("date") LocalDateTime date, @Param("userId") Long userId);
}
