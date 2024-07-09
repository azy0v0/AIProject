package com.springaichatgpt.dto;

import com.springaichatgpt.pojo.User;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;

@MappedSuperclass
public interface BaseEntity extends BaseDateTime {

    @Id
    @GeneratedValue(generatorType = UUIDIdGenerator.class)
    String id();

    @ManyToOne
    @OnDissociate(DissociateAction.DELETE)
    User editor();

    @ManyToOne
    @OnDissociate(DissociateAction.DELETE)
    User creator();
}