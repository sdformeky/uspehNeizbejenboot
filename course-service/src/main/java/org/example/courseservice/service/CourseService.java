package org.example.courseservice.service;

import lombok.RequiredArgsConstructor;
import org.example.courseservice.dto.CourseDto;
import org.example.courseservice.exception.NotFoundException;
import org.example.courseservice.repository.CourseRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;

    public List<CourseDto> findAll() {
        return courseRepository.findAll(Sort.by("id")).stream()
                .map(CourseDto::from)
                .toList();
    }

    public CourseDto findById(Long id) {
        return courseRepository.findById(id)
                .map(CourseDto::from)
                .orElseThrow(() -> new NotFoundException("Курс с id " + id + " не найден"));
    }
}
