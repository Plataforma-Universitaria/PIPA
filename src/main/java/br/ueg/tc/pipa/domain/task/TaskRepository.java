package br.ueg.tc.pipa.domain.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> getTaskByDateAndUser_Id(LocalDateTime date, Long userId);
    Optional<List<Task>> getTasksByUser_Id(Long userId);

    @Modifying
    @Query("delete from Task d where d.id = :taskId and d.user.id = :userId")
    void deleteByIdAndUserId(Long taskId, Long userId);
}
