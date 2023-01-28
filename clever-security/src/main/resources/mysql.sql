/* ====================================================================================================================
     --
==================================================================================================================== */
create table auto_increment_id
(
    id                  bigint                  not null        auto_increment                          comment '主键id',
    sequence_name       varchar(127)    binary  not null                                                comment '序列名称',
    current_value       bigint                  not null        default -1                              comment '当前值',
    description         varchar(511)                                                                    comment '说明',
    create_at           datetime(3)             not null        default current_timestamp(3)            comment '创建时间',
    update_at           datetime(3)                             on update current_timestamp(3)          comment '更新时间',
    primary key (id)
) comment = '自增长id表';
create unique index idx_auto_increment_id_sequence_name on auto_increment_id (sequence_name);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
     --
==================================================================================================================== */






