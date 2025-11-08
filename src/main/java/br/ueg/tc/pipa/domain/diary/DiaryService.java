package br.ueg.tc.pipa.domain.diary;

import br.ueg.tc.pipa.domain.user.User;
import br.ueg.tc.pipa.domain.user.UserService;
import br.ueg.tc.pipa.features.dto.DiaryDTO;
import br.ueg.tc.pipa.infra.utils.DateFormatter;
import br.ueg.tc.pipa_integrator.exceptions.diary.DiaryNotFoundException;
import br.ueg.tc.pipa_integrator.exceptions.serviceProvider.MandatoryParameterNotFilled;
import br.ueg.tc.pipa_integrator.exceptions.user.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DiaryService {

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private UserService userService;

    public Diary create(DiaryDTO diaryDTO) {

        validateDiary(diaryDTO);
        User user = userService.findById(diaryDTO.userUuid());
        validateDiaryToCreate(diaryDTO);
        Diary diary = new Diary();
        diary.setDate(diaryDTO.date());
        diary.setNote(diaryDTO.note());
        diary.setUser(user);
        diary = diaryRepository.saveAndFlush(diary);

        return diaryRepository.findById(diary.getId()).orElseThrow(UserNotFoundException::new);
    }

    @Transactional
    public Diary delete(LocalDateTime date, Long userId) {
        List<Diary> diaries = diaryRepository.getDiariesByUser_Id(userId).orElseThrow(DiaryNotFoundException::new);

        Diary diary = diaries.stream().findFirst().filter(
                        diaryToDelete -> diaryToDelete.getDate().equals(date))
                .orElseThrow(DiaryNotFoundException::new);
        diaryRepository.deleteById(diary.getId());

        return diary;
    }

    private void validateDiary(DiaryDTO diaryDTO) {
        if (diaryDTO.date() == null || diaryDTO.note() == null || diaryDTO.userUuid() == null)
            throw new MandatoryParameterNotFilled("Data e a descrição da nota devem ser fornecidas");

    }
    private void validateDiaryToCreate(DiaryDTO diaryDTO) {
        if (findByDate(diaryDTO.date(), diaryDTO.userUuid()) != null)
            throw new MandatoryParameterNotFilled(
                    STR."Já existe uma nota para esse dia, se deseja atualizar solicite '*Atualizar solicitação do dia \{DateFormatter.format(diaryDTO.date())}para: {{Sua nova anotação}}'*");

    }

    public List<Diary> findAllByUserId(Long userId) {
        return diaryRepository.getDiariesByUser_Id(userId).orElseThrow(DiaryNotFoundException::new);
    }

    public Diary findByDate(LocalDateTime date, Long userId) {
        return diaryRepository.getDiaryByDateAndUser_Id(date, userId).orElse(null);
    }

    @Transactional
    public Diary update(DiaryDTO diaryDTO, Long diaryId) {

        validateDiary(diaryDTO);
        User user = userService.findById(diaryDTO.userUuid());
        Diary diary = new Diary();
        diary.setDate(diaryDTO.date());
        diary.setNote(diaryDTO.note());
        diary.setUser(user);
        diary.setId(diaryId);
        diary = diaryRepository.saveAndFlush(diary);

        return diaryRepository.findById(diary.getId()).orElseThrow(UserNotFoundException::new);
    }
}
