package com.example.chillisauce.spaces.entity;

import com.example.chillisauce.spaces.dto.BoxRequestDto;
import com.example.chillisauce.users.entity.User;
import lombok.*;


import javax.persistence.*;
import javax.validation.constraints.NotEmpty;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Builder
@AllArgsConstructor
public class Box {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    @NotEmpty
    private String boxName;
    @Column(nullable = false)
    @NotEmpty
    private String x;
    @Column(nullable = false)
    @NotEmpty
    private String y;

    private String username;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id")
    private Space space;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public User user;

    public Box(BoxRequestDto boxRequestDto) {
        this.boxName = boxRequestDto.getBoxName();
        this.x = boxRequestDto.getX();
        this.y = boxRequestDto.getY();
    }

    public void updateBox(BoxRequestDto boxRequestDto) {
        this.boxName = boxRequestDto.getBoxName();
        this.x = boxRequestDto.getX();
        this.y = boxRequestDto.getY();
    }

    public void linkSpace(Space space) {
        this.space = space;
        space.getBoxes().add(this);
    }

    public void updateBox(BoxRequestDto boxRequestDto, User user) {
        this.boxName = boxRequestDto.getBoxName();
        this.x = boxRequestDto.getX();
        this.y = boxRequestDto.getY();
        this.user = user;
        this.username = user.getUsername();
    }
}
