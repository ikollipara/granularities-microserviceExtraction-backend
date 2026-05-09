--
-- PostgreSQL database dump
--

-- Dumped by pg_dump version 18.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: class; Type: TABLE; Schema: public; Owner: thesis
--

CREATE TABLE public.class (
    id bigint NOT NULL,
    file_path oid,
    name character varying(255),
    package_name oid,
    relative_file_path oid,
    repo_id bigint
);


ALTER TABLE public.class OWNER TO thesis;

--
-- Name: class_node; Type: TABLE; Schema: public; Owner: thesis
--

CREATE TABLE public.class_node (
    primary_key bigint NOT NULL,
    id character varying(255)
);


ALTER TABLE public.class_node OWNER TO thesis;

--
-- Name: class_node_neighbors; Type: TABLE; Schema: public; Owner: thesis
--

CREATE TABLE public.class_node_neighbors (
    class_node_primary_key bigint NOT NULL,
    neighbors_id bigint NOT NULL
);


ALTER TABLE public.class_node_neighbors OWNER TO thesis;

--
-- Name: class_node_seq; Type: SEQUENCE; Schema: public; Owner: thesis
--

CREATE SEQUENCE public.class_node_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.class_node_seq OWNER TO thesis;

--
-- Name: class_seq; Type: SEQUENCE; Schema: public; Owner: thesis
--

CREATE SEQUENCE public.class_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.class_seq OWNER TO thesis;

--
-- Name: component; Type: TABLE; Schema: public; Owner: thesis
--

CREATE TABLE public.component (
    id bigint NOT NULL
);


ALTER TABLE public.component OWNER TO thesis;

--
-- Name: component_nodes; Type: TABLE; Schema: public; Owner: thesis
--

CREATE TABLE public.component_nodes (
    component_id bigint NOT NULL,
    nodes_primary_key bigint NOT NULL
);


ALTER TABLE public.component_nodes OWNER TO thesis;

--
-- Name: component_seq; Type: SEQUENCE; Schema: public; Owner: thesis
--

CREATE SEQUENCE public.component_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.component_seq OWNER TO thesis;

--
-- Name: decomposition; Type: TABLE; Schema: public; Owner: thesis
--

CREATE TABLE public.decomposition (
    id bigint NOT NULL,
    parameters_id bigint,
    repository_id bigint
);


ALTER TABLE public.decomposition OWNER TO thesis;

--
-- Name: decomposition_parameters; Type: TABLE; Schema: public; Owner: thesis
--

CREATE TABLE public.decomposition_parameters (
    id bigint NOT NULL,
    contributor_coupling boolean NOT NULL,
    granularity smallint,
    interval_seconds integer NOT NULL,
    logical_coupling boolean NOT NULL,
    num_services integer NOT NULL,
    semantic_coupling boolean NOT NULL,
    size_threshold integer NOT NULL,
    tree_sitter boolean NOT NULL,
    CONSTRAINT decomposition_parameters_granularity_check CHECK (((granularity >= 0) AND (granularity <= 2)))
);


ALTER TABLE public.decomposition_parameters OWNER TO thesis;

--
-- Name: decomposition_parameters_seq; Type: SEQUENCE; Schema: public; Owner: thesis
--

CREATE SEQUENCE public.decomposition_parameters_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.decomposition_parameters_seq OWNER TO thesis;

--
-- Name: decomposition_seq; Type: SEQUENCE; Schema: public; Owner: thesis
--

CREATE SEQUENCE public.decomposition_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.decomposition_seq OWNER TO thesis;

--
-- Name: decomposition_services; Type: TABLE; Schema: public; Owner: thesis
--

CREATE TABLE public.decomposition_services (
    decomposition_id bigint NOT NULL,
    services_id bigint NOT NULL
);


ALTER TABLE public.decomposition_services OWNER TO thesis;

--
-- Name: evaluation_metrics; Type: TABLE; Schema: public; Owner: thesis
--

CREATE TABLE public.evaluation_metrics (
    id bigint NOT NULL,
    average_class_number double precision NOT NULL,
    average_loc double precision NOT NULL,
    contributor_overlapping double precision NOT NULL,
    contributors_per_microservice double precision NOT NULL,
    execution_time_millis_clustering bigint NOT NULL,
    execution_time_millis_strategy bigint NOT NULL,
    granularity smallint,
    similarity double precision NOT NULL,
    decomposition_id bigint,
    CONSTRAINT evaluation_metrics_granularity_check CHECK (((granularity >= 0) AND (granularity <= 2)))
);


ALTER TABLE public.evaluation_metrics OWNER TO thesis;

--
-- Name: evaluation_metrics_seq; Type: SEQUENCE; Schema: public; Owner: thesis
--

CREATE SEQUENCE public.evaluation_metrics_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.evaluation_metrics_seq OWNER TO thesis;

--
-- Name: git_repository; Type: TABLE; Schema: public; Owner: thesis
--

CREATE TABLE public.git_repository (
    id bigint NOT NULL,
    name character varying(255),
    remote_path character varying(255)
);


ALTER TABLE public.git_repository OWNER TO thesis;

--
-- Name: git_repository_seq; Type: SEQUENCE; Schema: public; Owner: thesis
--

CREATE SEQUENCE public.git_repository_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.git_repository_seq OWNER TO thesis;

--
-- Name: microservice; Type: TABLE; Schema: public; Owner: thesis
--

CREATE TABLE public.microservice (
    id bigint NOT NULL,
    hash character varying(255),
    score integer NOT NULL
);


ALTER TABLE public.microservice OWNER TO thesis;

--
-- Name: microservice_class_files; Type: TABLE; Schema: public; Owner: thesis
--

CREATE TABLE public.microservice_class_files (
    microservice_id bigint NOT NULL,
    class_files character varying(255)
);


ALTER TABLE public.microservice_class_files OWNER TO thesis;

--
-- Name: microservice_metrics; Type: TABLE; Schema: public; Owner: thesis
--

CREATE TABLE public.microservice_metrics (
    id bigint NOT NULL,
    loc integer NOT NULL,
    microservice_id bigint
);


ALTER TABLE public.microservice_metrics OWNER TO thesis;

--
-- Name: microservice_metrics_contributors; Type: TABLE; Schema: public; Owner: thesis
--

CREATE TABLE public.microservice_metrics_contributors (
    microservice_metrics_id bigint CONSTRAINT microservice_metrics_contribut_microservice_metrics_id_not_null NOT NULL,
    contributors character varying(255)
);


ALTER TABLE public.microservice_metrics_contributors OWNER TO thesis;

--
-- Name: microservice_metrics_seq; Type: SEQUENCE; Schema: public; Owner: thesis
--

CREATE SEQUENCE public.microservice_metrics_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.microservice_metrics_seq OWNER TO thesis;

--
-- Name: microservice_relations; Type: TABLE; Schema: public; Owner: thesis
--

CREATE TABLE public.microservice_relations (
    microservice_id bigint NOT NULL,
    relations_id bigint NOT NULL
);


ALTER TABLE public.microservice_relations OWNER TO thesis;

--
-- Name: microservice_seq; Type: SEQUENCE; Schema: public; Owner: thesis
--

CREATE SEQUENCE public.microservice_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.microservice_seq OWNER TO thesis;

--
-- Name: node_weight_pair; Type: TABLE; Schema: public; Owner: thesis
--

CREATE TABLE public.node_weight_pair (
    id bigint NOT NULL,
    weight double precision NOT NULL,
    node_primary_key bigint
);


ALTER TABLE public.node_weight_pair OWNER TO thesis;

--
-- Name: node_weight_pair_seq; Type: SEQUENCE; Schema: public; Owner: thesis
--

CREATE SEQUENCE public.node_weight_pair_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.node_weight_pair_seq OWNER TO thesis;

--
-- Name: class_node class_node_pkey; Type: CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.class_node
    ADD CONSTRAINT class_node_pkey PRIMARY KEY (primary_key);


--
-- Name: class class_pkey; Type: CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.class
    ADD CONSTRAINT class_pkey PRIMARY KEY (id);


--
-- Name: component component_pkey; Type: CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.component
    ADD CONSTRAINT component_pkey PRIMARY KEY (id);


--
-- Name: decomposition_parameters decomposition_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.decomposition_parameters
    ADD CONSTRAINT decomposition_parameters_pkey PRIMARY KEY (id);


--
-- Name: decomposition decomposition_pkey; Type: CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.decomposition
    ADD CONSTRAINT decomposition_pkey PRIMARY KEY (id);


--
-- Name: decomposition_services decomposition_services_pkey; Type: CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.decomposition_services
    ADD CONSTRAINT decomposition_services_pkey PRIMARY KEY (decomposition_id, services_id);


--
-- Name: evaluation_metrics evaluation_metrics_pkey; Type: CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.evaluation_metrics
    ADD CONSTRAINT evaluation_metrics_pkey PRIMARY KEY (id);


--
-- Name: git_repository git_repository_pkey; Type: CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.git_repository
    ADD CONSTRAINT git_repository_pkey PRIMARY KEY (id);


--
-- Name: microservice_metrics microservice_metrics_pkey; Type: CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.microservice_metrics
    ADD CONSTRAINT microservice_metrics_pkey PRIMARY KEY (id);


--
-- Name: microservice microservice_pkey; Type: CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.microservice
    ADD CONSTRAINT microservice_pkey PRIMARY KEY (id);


--
-- Name: node_weight_pair node_weight_pair_pkey; Type: CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.node_weight_pair
    ADD CONSTRAINT node_weight_pair_pkey PRIMARY KEY (id);


--
-- Name: evaluation_metrics uk4tlu1asr0f1rcgshncpng4fls; Type: CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.evaluation_metrics
    ADD CONSTRAINT uk4tlu1asr0f1rcgshncpng4fls UNIQUE (decomposition_id);


--
-- Name: class uka3fl6ugqfue2l06rdbkl6qdnn; Type: CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.class
    ADD CONSTRAINT uka3fl6ugqfue2l06rdbkl6qdnn UNIQUE (file_path);


--
-- Name: class ukb1kom8uej70ga9rtkvye9dy3r; Type: CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.class
    ADD CONSTRAINT ukb1kom8uej70ga9rtkvye9dy3r UNIQUE (relative_file_path);


--
-- Name: decomposition_services ukd3bdnab5wm7tga6is5gqn9y6h; Type: CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.decomposition_services
    ADD CONSTRAINT ukd3bdnab5wm7tga6is5gqn9y6h UNIQUE (services_id);


--
-- Name: component_nodes ukmwv9rx3mj8dca44b5v3ebhl45; Type: CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.component_nodes
    ADD CONSTRAINT ukmwv9rx3mj8dca44b5v3ebhl45 UNIQUE (nodes_primary_key);


--
-- Name: microservice_metrics uknhpp2nig8dgd9cxro01qf120l; Type: CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.microservice_metrics
    ADD CONSTRAINT uknhpp2nig8dgd9cxro01qf120l UNIQUE (microservice_id);


--
-- Name: microservice uksmq9tkdad7vgc7b021vhexthi; Type: CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.microservice
    ADD CONSTRAINT uksmq9tkdad7vgc7b021vhexthi UNIQUE (hash);


--
-- Name: class_node_neighbors fk13o94pj7bdbaaelk43ccgcqs; Type: FK CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.class_node_neighbors
    ADD CONSTRAINT fk13o94pj7bdbaaelk43ccgcqs FOREIGN KEY (neighbors_id) REFERENCES public.node_weight_pair(id);


--
-- Name: component_nodes fk1424jx2xc4msbxix2cs7h2v79; Type: FK CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.component_nodes
    ADD CONSTRAINT fk1424jx2xc4msbxix2cs7h2v79 FOREIGN KEY (nodes_primary_key) REFERENCES public.class_node(primary_key);


--
-- Name: microservice_metrics_contributors fk211it4r752jgm1d7s23qthnrj; Type: FK CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.microservice_metrics_contributors
    ADD CONSTRAINT fk211it4r752jgm1d7s23qthnrj FOREIGN KEY (microservice_metrics_id) REFERENCES public.microservice_metrics(id);


--
-- Name: class_node_neighbors fk2u5icyx8qhr3wi2g4tyt73cve; Type: FK CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.class_node_neighbors
    ADD CONSTRAINT fk2u5icyx8qhr3wi2g4tyt73cve FOREIGN KEY (class_node_primary_key) REFERENCES public.class_node(primary_key);


--
-- Name: class fk41pojk88tqrw98dpv4gmcfjdl; Type: FK CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.class
    ADD CONSTRAINT fk41pojk88tqrw98dpv4gmcfjdl FOREIGN KEY (repo_id) REFERENCES public.git_repository(id) ON DELETE CASCADE;


--
-- Name: decomposition_services fk4u3nyp6qbkrpv32cojxm5txmh; Type: FK CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.decomposition_services
    ADD CONSTRAINT fk4u3nyp6qbkrpv32cojxm5txmh FOREIGN KEY (decomposition_id) REFERENCES public.decomposition(id);


--
-- Name: microservice_relations fkcv82o13cl58ddsc0ia9a03s2f; Type: FK CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.microservice_relations
    ADD CONSTRAINT fkcv82o13cl58ddsc0ia9a03s2f FOREIGN KEY (relations_id) REFERENCES public.microservice(id);


--
-- Name: decomposition_services fkd02yeqslcq72unic6j311wd80; Type: FK CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.decomposition_services
    ADD CONSTRAINT fkd02yeqslcq72unic6j311wd80 FOREIGN KEY (services_id) REFERENCES public.component(id);


--
-- Name: microservice_metrics fkdllk7sdl10ckqxn4d8gogkds5; Type: FK CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.microservice_metrics
    ADD CONSTRAINT fkdllk7sdl10ckqxn4d8gogkds5 FOREIGN KEY (microservice_id) REFERENCES public.component(id);


--
-- Name: node_weight_pair fkf6db0apdq96kj6fmgsj2mtkax; Type: FK CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.node_weight_pair
    ADD CONSTRAINT fkf6db0apdq96kj6fmgsj2mtkax FOREIGN KEY (node_primary_key) REFERENCES public.class_node(primary_key);


--
-- Name: evaluation_metrics fkfyuruhpo8u704hdcj3ypn9cg2; Type: FK CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.evaluation_metrics
    ADD CONSTRAINT fkfyuruhpo8u704hdcj3ypn9cg2 FOREIGN KEY (decomposition_id) REFERENCES public.decomposition(id);


--
-- Name: decomposition fkj0onpms78o71yletji142gism; Type: FK CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.decomposition
    ADD CONSTRAINT fkj0onpms78o71yletji142gism FOREIGN KEY (repository_id) REFERENCES public.git_repository(id);


--
-- Name: microservice_class_files fkkkeiabnrg104461xmlxs2t2h2; Type: FK CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.microservice_class_files
    ADD CONSTRAINT fkkkeiabnrg104461xmlxs2t2h2 FOREIGN KEY (microservice_id) REFERENCES public.microservice(id);


--
-- Name: microservice_relations fklkl9andmn9cn77dyx836qlaoy; Type: FK CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.microservice_relations
    ADD CONSTRAINT fklkl9andmn9cn77dyx836qlaoy FOREIGN KEY (microservice_id) REFERENCES public.microservice(id);


--
-- Name: component_nodes fkmyfp6sy2b2ij7884dcnps7ysm; Type: FK CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.component_nodes
    ADD CONSTRAINT fkmyfp6sy2b2ij7884dcnps7ysm FOREIGN KEY (component_id) REFERENCES public.component(id);


--
-- Name: decomposition fkqiq2ro1wypu1es0sj84xq28ch; Type: FK CONSTRAINT; Schema: public; Owner: thesis
--

ALTER TABLE ONLY public.decomposition
    ADD CONSTRAINT fkqiq2ro1wypu1es0sj84xq28ch FOREIGN KEY (parameters_id) REFERENCES public.decomposition_parameters(id);


--
-- PostgreSQL database dump complete
--


