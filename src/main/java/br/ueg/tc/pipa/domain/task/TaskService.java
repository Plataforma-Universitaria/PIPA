package br.ueg.tc.pipa.domain.task;

import br.ueg.tc.pipa.domain.user.User;
import br.ueg.tc.pipa.domain.user.UserService;
import br.ueg.tc.pipa.features.dto.TaskDTO;
import br.ueg.tc.pipa.infra.utils.DateFormatter;
import br.ueg.tc.pipa_integrator.exceptions.serviceProvider.MandatoryParameterNotFilled;
import br.ueg.tc.pipa_integrator.exceptions.task.TaskException;
import br.ueg.tc.pipa_integrator.exceptions.user.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserService userService;

    public Task create(TaskDTO TaskDTO) {

        validateTask(TaskDTO);
        User user = userService.findById(TaskDTO.userUuid());
        validateTaskToCreate(TaskDTO);
        Task task = new Task();
        task.setDate(TaskDTO.date());
        task.setNote(TaskDTO.note());
        task.setUser(user);
        task = taskRepository.saveAndFlush(task);

        return taskRepository.findById(task.getId()).orElseThrow(UserNotFoundException::new);
    }

    @Transactional
    public void delete(Long taskId) {
        taskRepository.deleteById(taskId);
    }

    private void validateTask(TaskDTO TaskDTO) {
        if (TaskDTO.date() == null || TaskDTO.note() == null || TaskDTO.userUuid() == null)
            throw new TaskException("Data e a descrição da nota devem ser fornecidas");

    }

    private void validateTaskToCreate(TaskDTO TaskDTO) {
        if (findByDate(TaskDTO.date(), TaskDTO.userUuid()) != null)
            throw new TaskException("Já existe uma nota para esse dia, se deseja atualizar solicite '*Atualizar solicitação do dia " + DateFormatter.format(TaskDTO.date()) + " para: {{Sua nova anotação}}'*");

    }

    public List<Task> findAllByUserId(Long userId) {
        return taskRepository.getTasksByUser_Id(userId).orElse(null);
    }

    public Task findByDate(LocalDateTime date, Long userId) {
        return taskRepository.getTaskByDateAndUser_Id(date, userId).orElse(null);
    }

    @Transactional
    public Task update(TaskDTO TaskDTO, Long TaskId) {

        validateTask(TaskDTO);
        User user = userService.findById(TaskDTO.userUuid());
        Task task = new Task();
        task.setDate(TaskDTO.date());
        task.setNote(TaskDTO.note());
        task.setUser(user);
        task.setId(TaskId);
        task = taskRepository.saveAndFlush(task);

        return taskRepository.findById(task.getId()).orElseThrow(UserNotFoundException::new);
    }
}
