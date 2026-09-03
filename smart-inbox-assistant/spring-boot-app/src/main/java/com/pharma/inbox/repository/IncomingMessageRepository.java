package com.pharma.inbox.repository;

import com.pharma.inbox.model.IncomingMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;
import java.util.Optional;

@Repository
public class IncomingMessageRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public IncomingMessageRepository(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    private static final RowMapper<IncomingMessage> ROW_MAPPER = (rs, rowNum) -> {
        IncomingMessage msg = new IncomingMessage();
        msg.setId(rs.getLong("id"));
        msg.setSender(rs.getString("sender"));
        msg.setSubject(rs.getString("subject"));
        msg.setReceivedAt(rs.getTimestamp("received_at"));
        msg.setBodyText(rs.getClob("body_text"));
        msg.setRawSourceRef(rs.getString("raw_source_ref"));
        msg.setProcessedFlag(rs.getInt("processed_flag"));
        return msg;
    };

    public Long save(IncomingMessage message) {
        String sql = """
            INSERT INTO INCOMING_MESSAGE (sender, subject, received_at, body_text, raw_source_ref, processed_flag)
            VALUES (:sender, :subject, :receivedAt, :bodyText, :rawSourceRef, :processedFlag)
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("sender", message.getSender())
            .addValue("subject", message.getSubject())
            .addValue("receivedAt", message.getReceivedAt() != null ? message.getReceivedAt() : new Timestamp(System.currentTimeMillis()))
            .addValue("bodyText", message.getBodyText())
            .addValue("rawSourceRef", message.getRawSourceRef())
            .addValue("processedFlag", message.getProcessedFlag());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        namedParameterJdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
        
        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }

    public List<IncomingMessage> findAllUnprocessed() {
        String sql = "SELECT * FROM INCOMING_MESSAGE WHERE processed_flag = 0 ORDER BY received_at ASC";
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    public List<IncomingMessage> findAll(int limit, int offset) {
        String sql = "SELECT * FROM INCOMING_MESSAGE ORDER BY received_at DESC FETCH FIRST ? ROWS ONLY OFFSET ? ROWS";
        return jdbcTemplate.query(sql, ROW_MAPPER, limit, offset);
    }

    public Optional<IncomingMessage> findById(Long id) {
        String sql = "SELECT * FROM INCOMING_MESSAGE WHERE id = ?";
        try {
            IncomingMessage msg = jdbcTemplate.queryForObject(sql, ROW_MAPPER, id);
            return Optional.ofNullable(msg);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void markAsProcessed(Long id) {
        String sql = "UPDATE INCOMING_MESSAGE SET processed_flag = 1 WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM INCOMING_MESSAGE";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
