package br.ueg.tc.pipa.publicServices;


import br.ueg.tc.pipa.domain.task.Task;
import br.ueg.tc.pipa.domain.task.TaskService;
import br.ueg.tc.pipa.domain.user.UserService;
import br.ueg.tc.pipa.features.dto.TaskDTO;
import br.ueg.tc.pipa.infra.utils.DateFormatter;
import br.ueg.tc.pipa_integrator.annotations.ServiceProviderMethod;
import br.ueg.tc.pipa_integrator.exceptions.task.TaskException;
import br.ueg.tc.pipa_integrator.interfaces.providers.service.IServiceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class PublicService implements IServiceProvider {

    @Autowired
    UserService userService;

    @Autowired
    TaskService taskService;

    @ServiceProviderMethod(manipulatesData = true,
            actionName = "Adicionar uma anotação",
            activationPhrases = {"Crie uma anotação para o dia 17 desse mês(formato do párâmetro é YYYY-MM-DDTHH:00:00) assim : Apresentação do tc",
                    "Adicione uma nota para o dia 5 de dezembro como entrega de entrega das notas de gestão",
                    "Marque um lembrete para depois de amanhã(formato do párâmetro é YYYY-MM-DDTHH:00:00): Reunião com o orientador às 14h.",
                    "Crie uma nota para a próxima sexta-(formato do párâmetro é YYYY-MM-DDTHH:00:00): Entregar a primeira versão do documento.",
                    "Adicione uma anotação para o dia 20 de janeiro(formato do párâmetro é YYYY-MM-DDTHH:00:00): Início das inscrições no congresso.",
                    "Lembrete para 3 dias antes do Natal(formato do parâmetro é YYYY-MM-DDTHH:00:00): Comprar os presentes.",
                    "Anote para a primeira segunda-feira do mês que vem(formato do párâmetro é YYYY-MM-DDTHH:00:00): Enviar relatório de progresso."}, addSpec = "String date formato YYYY-MM-DDTHH:00:00, String note,")
    public String addTask(String date, String note) {
        LocalDateTime dateTime = LocalDateTime.parse(date);
        Long id = userService.getCurrentUserId();
        TaskDTO taskDTO = new TaskDTO(note, dateTime, id);
        try {
            Task task = taskService.create(taskDTO);
            return task.toString();
        } catch (TaskException e) {
            return e.getMessage();
        }
    }

    @ServiceProviderMethod(manipulatesData = true,
            actionName = "Editar uma anotação",
            activationPhrases = {"Edite a anotação do dia 17 desse mês(formato do párâmetro é YYYY-MM-DDTHH:00:00) assim : Apresentação do tc pra adicionar o slide no envio",
                    "Alterar nota para o dia 5 de dezembro como entrega de entrega das notas de gestão e governaça",
                    "Atualizar o lembrete para depois de amanhã(formato do párâmetro é YYYY-MM-DDTHH:00:00): Reunião com o orientador às 17h.",
                    "Refazer nota para a próxima sexta-(formato do párâmetro é YYYY-MM-DDTHH:00:00): Entregar a primeira versão do documento corrigida.",
                    "Editar anotação para o dia 20 de janeiro(formato do párâmetro é YYYY-MM-DDTHH:00:00): Início das inscrições no congresso.",
                    "Atualiza pra mim o lembrete do dia 3 dias antes do Natal(formato do parâmetro é YYYY-MM-DDTHH:00:00): Comprar os presentes."},
            addSpec = "String date formato YYYY-MM-DDTHH:00:00, String note")
    public String updateTask(String date, String note) {
        LocalDateTime dateTime = LocalDateTime.parse(date);
        Long id;
        Long taskId;
        try {
            id = userService.getCurrentUserId();
            taskId = taskService.findByDate(dateTime, id).getId();

        } catch (RuntimeException ne) {
            return "Não encontrei anotação para o dia " + DateFormatter.format(dateTime);
        }
        TaskDTO taskDTO = new TaskDTO(note, dateTime, id);
        Task task = taskService.update(taskDTO, taskId);
        return task.toString();
    }

    @ServiceProviderMethod(manipulatesData = true,
            actionName = "Excluir uma anotação",
            activationPhrases = {"Delete a anotação do dia 5/06/2025(formato do parâmetro é YYYY-MM-DDTHH:00:00)",
                    "Quero apagar a anotação do dia 27(formato do parâmetro é YYYY-MM-DDTHH:00:00)",
                    "Apaga pra mim a nota do dia 15/10(formato do parâmetro é YYYY-MM-DDTHH:00:00)",
                    "Delete a nota do dia 16(formato do parâmetro é YYYY-MM-DDTHH:00:00)"}, addSpec = "String date formato YYYY-MM-DDTHH:00:00")
    public String deleteTask(String date) {
        Task task = taskService.findByDate(LocalDateTime.parse(date), userService.getCurrentUserId());
        if (task == null) {
            return "Não encontrei essa anotação.";
        }
        Long taskId = task.getId();
        taskService.delete(taskId);
        return "A sua anotação do dia " + DateFormatter.format(task.getDate()) + " foi excluída com sucesso!";
    }

    @ServiceProviderMethod(
            actionName = "Consultar uma anotação",
            activationPhrases = {"Quero a anotação do dia 5/06/2025(formato do parâmetro é YYYY-MM-DDTHH:00:00)",
                    "Anotação do dia 6 (formato do parâmetro é YYYY-MM-DDTHH:00:00)",
                    "O que eu tenho marcado para amanhã?(formato do parâmetro é YYYY-MM-DDTHH:00:00)",
                    "Consultar anotação do dia 5.(formato do parâmetro é YYYY-MM-DDTHH:00:00)",
                    "Pega pra mim a agenda do dia 15 (formato do parâmetro é YYYY-MM-DDTHH:00:00)"}, addSpec = "String date formato YYYY-MM-DDTHH:00:00")
    public String getTask(String date) {
        Task task = taskService.findByDate(LocalDateTime.parse(date), userService.getCurrentUserId());
        if (task == null) {
            return "Não encontrei essa anotação.";
        }
        return task.toString();
    }

    @ServiceProviderMethod(
            actionName = "Consultar uma anotação",
            activationPhrases = {"Quero a anotação do dia 5/06/2025(formato do parâmetro é YYYY-MM-DDTHH:00:00)",
                    "Anotação do dia 6 (formato do parâmetro é YYYY-MM-DDTHH:00:00)",
                    "Pega pra mim a agenda do dia 15 (formato do parâmetro é YYYY-MM-DDTHH:00:00)"}, addSpec = "String date formato YYYY-MM-DDTHH:00:00")
    public String getAllTasks() {
        List<Task> tasks = taskService.findAllByUserId(userService.getCurrentUserId());
        StringBuilder stringBuilder = new StringBuilder();
        if (tasks.isEmpty()) {
            return "Você não tem anotações.";
        }
        stringBuilder.append("Aqui estão suas anotações:\n");
        tasks.forEach(task -> stringBuilder.append(" \n---------------\n" +
                task.toString() + "\n"));
        return stringBuilder.toString();
    }

}
