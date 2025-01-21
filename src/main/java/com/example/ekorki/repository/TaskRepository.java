//package com.example.ekorki.repository;
//
//import com.example.ekorki.entity.TaskEntity;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.stereotype.Repository;
//
//@Repository
//public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
//    @Query(value = """
//        SELECT t.* FROM e_korki.tasks t
//        WHERE t.is_public = true
//        AND t.status IN ('OPEN', 'ASSIGNED')
//        AND t.end_date > NOW()
//        AND t.is_active = true
//        ORDER BY t.end_date ASC
//        """,
//            countQuery = """
//            SELECT COUNT(*) FROM e_korki.tasks t
//            WHERE t.is_public = true
//            AND t.status IN ('OPEN', 'ASSIGNED')
//            AND t.end_date > NOW()
//            AND t.is_active = true
//            """,
//            nativeQuery = true)
//    Page<TaskEntity> findPublicActiveTasks(Pageable pageable);
//}
