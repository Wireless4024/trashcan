DROP TABLE IF EXISTS file_record;
CREATE TABLE IF NOT EXISTS file_record
(
    id           SERIAL PRIMARY KEY,
    hash         VARCHAR,
    filename     VARCHAR,
    content_type VARCHAR,
    expire       TIMESTAMP without time zone,
    quota        INT
);
CREATE INDEX file_hash_idx ON file_record (hash);
CREATE INDEX file_exp_idx ON file_record (expire);
CREATE INDEX file_quo_idx ON file_record (quota);


CREATE OR REPLACE PROCEDURE rm_file(fid varchar)
    LANGUAGE plpgsql
AS
$$
BEGIN
    DELETE FROM file_record WHERE id::varchar = fid;
    COMMIT;
END;
$$;

CREATE OR REPLACE PROCEDURE purge_file(ref refcursor)
    LANGUAGE plpgsql AS
$$
BEGIN
    OPEN ref FOR SELECT *
                 FROM file_record
                 WHERE (expire IS NOT NULL AND expire < now()) OR (quota IS NOT NULL AND quota < 1);
    DELETE FROM file_record WHERE (expire IS NOT NULL AND expire < now()) OR (quota IS NOT NULL AND quota < 1);
END;
$$;

-- to use function
/*
BEGIN ;
select purge_file('ref');
FETCH ALL IN "ref";
COMMIT ;
  */