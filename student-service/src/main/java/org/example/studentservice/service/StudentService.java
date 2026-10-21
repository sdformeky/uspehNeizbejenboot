package org.example.studentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.studentservice.dto.StudentDto;
import org.example.studentservice.dto.StudentRequest;
import org.example.studentservice.exception.ConflictException;
import org.example.studentservice.exception.NotFoundException;
import org.example.studentservice.model.Student;
import org.example.studentservice.model.StudentGroup;
import org.example.studentservice.model.StudentStatus;
import org.example.studentservice.repository.FavoriteRepository;
import org.example.studentservice.repository.StudentGroupRepository;
import org.example.studentservice.repository.StudentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentGroupRepository groupRepository;
    private final FavoriteRepository favoriteRepository;

    public List<StudentDto> findAll() {
        return studentRepository.findAll(Sort.by("id")).stream()
                .map(StudentDto::from)
                .toList();
    }

    public StudentDto findById(Long id) {
        return StudentDto.from(getStudent(id));
    }

    @Transactional
    public StudentDto create(StudentRequest request) {
        Student student = new Student();
        apply(student, request);
        Student saved = studentRepository.save(student);
        log.info("Создан студент id={} {} {}, статус {}", saved.getId(), saved.getLastName(), saved.getFirstName(), saved.getStatus());
        return StudentDto.from(saved);
    }

    @Transactional
    public StudentDto update(Long id, StudentRequest request) {
        Student student = getStudent(id);
        apply(student, request);
        log.info("Обновлён студент id={} {} {}, статус {}", id, student.getLastName(), student.getFirstName(), student.getStatus());
        return StudentDto.from(student);
    }

    @Transactional
    public void delete(Long id) {
        Student student = getStudent(id);
        favoriteRepository.deleteByStudentId(student.getId());
        studentRepository.delete(student);
        log.info("Удалён студент id={} {} {}", id, student.getLastName(), student.getFirstName());
    }

    private Student getStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Студент с id " + id + " не найден"));
    }

    private void apply(Student student, StudentRequest request) {
        if (request.userId() != null) {
            studentRepository.findByUserId(request.userId())
                    .filter(other -> !other.getId().equals(student.getId()))
                    .ifPresent(other -> {
                        throw new ConflictException("Пользователь " + request.userId() + " уже привязан к студенту " + other.getId());
                    });
        }
        StudentGroup group = null;
        if (request.groupId() != null) {
            group = groupRepository.findById(request.groupId())
                    .orElseThrow(() -> new NotFoundException("Группа с id " + request.groupId() + " не найдена"));
        }
        student.setUserId(request.userId());
        student.setGroup(group);
        student.setFirstName(request.firstName().trim());
        student.setLastName(request.lastName().trim());
        student.setPhone(request.phone());
        student.setBirthDate(request.birthDate());
        student.setStatus(request.status() != null ? request.status() : StudentStatus.ACTIVE);
    }
}
