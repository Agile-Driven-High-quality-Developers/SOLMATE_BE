package org.solmate.domain.social.entity;

import java.time.LocalDateTime;

import org.solmate.common.base.BaseEntity;
import org.solmate.domain.social.enums.MentoringStatus;
import org.solmate.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mentoring_relation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MentoringRelation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id", nullable = false)
    private User mentor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentee_id", nullable = false)
    private User mentee;

    @Enumerated(EnumType.STRING)
    @Column
    private MentoringStatus status;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Builder
    public MentoringRelation(User mentor, User mentee) {
        this.mentor = mentor;
        this.mentee = mentee;
        this.status = MentoringStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
    }

    public void accept() {
        this.status = MentoringStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = MentoringStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }
}
