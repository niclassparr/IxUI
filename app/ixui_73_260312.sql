--
-- PostgreSQL database dump
--

-- Dumped from database version 10.5
-- Dumped by pg_dump version 10.5

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: config_dsc; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.config_dsc (
    id integer NOT NULL,
    interface_pos text NOT NULL,
    emm integer DEFAULT 1 NOT NULL
);


ALTER TABLE public.config_dsc OWNER TO postgres;

--
-- Name: config_dsc_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.config_dsc_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.config_dsc_id_seq OWNER TO postgres;

--
-- Name: config_dsc_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.config_dsc_id_seq OWNED BY public.config_dsc.id;


--
-- Name: config_dvbc; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.config_dvbc (
    id integer NOT NULL,
    interface_pos text NOT NULL,
    freq integer NOT NULL,
    symb integer NOT NULL,
    constellation text DEFAULT 'qam256'::text NOT NULL,
    del text DEFAULT 'DVBC'::text NOT NULL
);


ALTER TABLE public.config_dvbc OWNER TO postgres;

--
-- Name: config_dvbc_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.config_dvbc_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.config_dvbc_id_seq OWNER TO postgres;

--
-- Name: config_dvbc_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.config_dvbc_id_seq OWNED BY public.config_dvbc.id;


--
-- Name: config_dvbudp; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.config_dvbudp (
    id integer NOT NULL,
    interface_pos text NOT NULL,
    in_ip text DEFAULT '0.0.0.0'::text NOT NULL,
    in_port integer DEFAULT 10000 NOT NULL
);


ALTER TABLE public.config_dvbudp OWNER TO postgres;

--
-- Name: config_dvbudp_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.config_dvbudp_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.config_dvbudp_id_seq OWNER TO postgres;

--
-- Name: config_dvbudp_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.config_dvbudp_id_seq OWNED BY public.config_dvbudp.id;


--
-- Name: config_emm; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.config_emm (
    emm integer NOT NULL,
    interface_pos text
);


ALTER TABLE public.config_emm OWNER TO postgres;

--
-- Name: config_eqam; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.config_eqam (
    id integer NOT NULL,
    interface_pos text NOT NULL,
    network_num integer DEFAULT 1 NOT NULL
);


ALTER TABLE public.config_eqam OWNER TO postgres;

--
-- Name: config_eqam_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.config_eqam_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.config_eqam_id_seq OWNER TO postgres;

--
-- Name: config_eqam_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.config_eqam_id_seq OWNED BY public.config_eqam.id;


--
-- Name: config_hdmi; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.config_hdmi (
    id integer NOT NULL,
    interface_pos text NOT NULL,
    format text DEFAULT '1080i50'::text NOT NULL
);


ALTER TABLE public.config_hdmi OWNER TO postgres;

--
-- Name: config_hdmi_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.config_hdmi_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.config_hdmi_id_seq OWNER TO postgres;

--
-- Name: config_hdmi_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.config_hdmi_id_seq OWNED BY public.config_hdmi.id;


--
-- Name: config_hls; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.config_hls (
    id integer NOT NULL,
    interface_pos text NOT NULL,
    max_bitrate integer DEFAULT 0
);


ALTER TABLE public.config_hls OWNER TO postgres;

--
-- Name: config_hls_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.config_hls_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.config_hls_id_seq OWNER TO postgres;

--
-- Name: config_hls_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.config_hls_id_seq OWNED BY public.config_hls.id;


--
-- Name: config_istr; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.config_istr (
    id integer NOT NULL,
    interface_pos text NOT NULL,
    presentation_url text
);


ALTER TABLE public.config_istr OWNER TO postgres;

--
-- Name: config_istr_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.config_istr_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.config_istr_id_seq OWNER TO postgres;

--
-- Name: config_istr_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.config_istr_id_seq OWNED BY public.config_istr.id;


--
-- Name: config_sat; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.config_sat (
    id integer NOT NULL,
    interface_pos text NOT NULL,
    lnb_type text NOT NULL,
    freq integer NOT NULL,
    pol text NOT NULL,
    symb integer NOT NULL,
    del text DEFAULT 'DVBS'::text NOT NULL,
    satno integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.config_sat OWNER TO postgres;

--
-- Name: config_sat_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.config_sat_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.config_sat_id_seq OWNER TO postgres;

--
-- Name: config_sat_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.config_sat_id_seq OWNED BY public.config_sat.id;


--
-- Name: config_ter; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.config_ter (
    id integer NOT NULL,
    interface_pos text NOT NULL,
    freq integer NOT NULL,
    bw integer DEFAULT 8 NOT NULL,
    del text DEFAULT 'DVBT'::text NOT NULL
);


ALTER TABLE public.config_ter OWNER TO postgres;

--
-- Name: config_ter_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.config_ter_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.config_ter_id_seq OWNER TO postgres;

--
-- Name: config_ter_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.config_ter_id_seq OWNED BY public.config_ter.id;


--
-- Name: config_webradio; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.config_webradio (
    id integer NOT NULL,
    interface_pos text NOT NULL,
    gain integer DEFAULT 0,
    webradio_url text
);


ALTER TABLE public.config_webradio OWNER TO postgres;

--
-- Name: config_webradio_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.config_webradio_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.config_webradio_id_seq OWNER TO postgres;

--
-- Name: config_webradio_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.config_webradio_id_seq OWNED BY public.config_webradio.id;


--
-- Name: forced_content; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.forced_content (
    id integer NOT NULL,
    enable boolean DEFAULT false NOT NULL,
    name text DEFAULT ''::text NOT NULL,
    networks integer DEFAULT 0 NOT NULL,
    volume integer DEFAULT '-1'::integer NOT NULL,
    ts_filename text DEFAULT ''::text NOT NULL,
    operation_mode integer DEFAULT 0 NOT NULL,
    signal_type integer DEFAULT 0 NOT NULL,
    signal_override integer DEFAULT 0 NOT NULL,
    signal_status integer DEFAULT 0 NOT NULL,
    com_status boolean DEFAULT false NOT NULL
);


ALTER TABLE public.forced_content OWNER TO postgres;

--
-- Name: interfaces; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.interfaces (
    pos text NOT NULL,
    name text,
    type text NOT NULL,
    active boolean DEFAULT false NOT NULL,
    scantime timestamp without time zone,
    multiband boolean DEFAULT false NOT NULL
);


ALTER TABLE public.interfaces OWNER TO postgres;

--
-- Name: nv; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.nv (
    id integer NOT NULL,
    name text NOT NULL,
    value text NOT NULL
);


ALTER TABLE public.nv OWNER TO postgres;

--
-- Name: nv_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.nv_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.nv_id_seq OWNER TO postgres;

--
-- Name: nv_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.nv_id_seq OWNED BY public.nv.id;


--
-- Name: routes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.routes (
    id integer NOT NULL,
    service_key text NOT NULL,
    output_name text NOT NULL,
    lcn integer DEFAULT 0 NOT NULL,
    dsc_pos text DEFAULT 'None'::text NOT NULL,
    mod_pos text DEFAULT 'None'::text NOT NULL,
    mod_pos_net2 text DEFAULT 'None'::text NOT NULL,
    out_sid integer DEFAULT 0 NOT NULL,
    out_ip text DEFAULT '0.0.0.0'::text NOT NULL,
    epg_id text NOT NULL,
    hls_enable boolean DEFAULT false NOT NULL
);


ALTER TABLE public.routes OWNER TO postgres;

--
-- Name: routes_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.routes_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.routes_id_seq OWNER TO postgres;

--
-- Name: routes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.routes_id_seq OWNED BY public.routes.id;


--
-- Name: services; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.services (
    id integer NOT NULL,
    interface_pos text NOT NULL,
    name text NOT NULL,
    key text NOT NULL,
    sid integer NOT NULL,
    type text NOT NULL,
    lang text,
    all_langs text[],
    scrambled boolean DEFAULT false NOT NULL,
    enable boolean NOT NULL,
    istr_url text,
    istr_video boolean,
    hls_url text,
    webradio_url text,
    epg_id text
);


ALTER TABLE public.services OWNER TO postgres;

--
-- Name: services_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.services_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.services_id_seq OWNER TO postgres;

--
-- Name: services_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.services_id_seq OWNED BY public.services.id;


--
-- Name: user_sessions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_sessions (
    id integer NOT NULL,
    created timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    user_id integer NOT NULL,
    session_key text NOT NULL
);


ALTER TABLE public.user_sessions OWNER TO postgres;

--
-- Name: user_sessions_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.user_sessions_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.user_sessions_id_seq OWNER TO postgres;

--
-- Name: user_sessions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.user_sessions_id_seq OWNED BY public.user_sessions.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id integer NOT NULL,
    name text NOT NULL,
    password text NOT NULL,
    edit boolean DEFAULT false NOT NULL,
    view boolean DEFAULT false NOT NULL
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.users_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.users_id_seq OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: config_dsc id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_dsc ALTER COLUMN id SET DEFAULT nextval('public.config_dsc_id_seq'::regclass);


--
-- Name: config_dvbc id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_dvbc ALTER COLUMN id SET DEFAULT nextval('public.config_dvbc_id_seq'::regclass);


--
-- Name: config_dvbudp id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_dvbudp ALTER COLUMN id SET DEFAULT nextval('public.config_dvbudp_id_seq'::regclass);


--
-- Name: config_eqam id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_eqam ALTER COLUMN id SET DEFAULT nextval('public.config_eqam_id_seq'::regclass);


--
-- Name: config_hdmi id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_hdmi ALTER COLUMN id SET DEFAULT nextval('public.config_hdmi_id_seq'::regclass);


--
-- Name: config_hls id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_hls ALTER COLUMN id SET DEFAULT nextval('public.config_hls_id_seq'::regclass);


--
-- Name: config_istr id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_istr ALTER COLUMN id SET DEFAULT nextval('public.config_istr_id_seq'::regclass);


--
-- Name: config_sat id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_sat ALTER COLUMN id SET DEFAULT nextval('public.config_sat_id_seq'::regclass);


--
-- Name: config_ter id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_ter ALTER COLUMN id SET DEFAULT nextval('public.config_ter_id_seq'::regclass);


--
-- Name: config_webradio id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_webradio ALTER COLUMN id SET DEFAULT nextval('public.config_webradio_id_seq'::regclass);


--
-- Name: nv id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.nv ALTER COLUMN id SET DEFAULT nextval('public.nv_id_seq'::regclass);


--
-- Name: routes id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.routes ALTER COLUMN id SET DEFAULT nextval('public.routes_id_seq'::regclass);


--
-- Name: services id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.services ALTER COLUMN id SET DEFAULT nextval('public.services_id_seq'::regclass);


--
-- Name: user_sessions id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_sessions ALTER COLUMN id SET DEFAULT nextval('public.user_sessions_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Data for Name: config_dsc; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.config_dsc (id, interface_pos, emm) FROM stdin;
1	x1a	1
2	x1b	2
\.


--
-- Data for Name: config_dvbc; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.config_dvbc (id, interface_pos, freq, symb, constellation, del) FROM stdin;
\.


--
-- Data for Name: config_dvbudp; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.config_dvbudp (id, interface_pos, in_ip, in_port) FROM stdin;
\.


--
-- Data for Name: config_emm; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.config_emm (emm, interface_pos) FROM stdin;
1	\N
2	\N
3	\N
4	\N
5	\N
\.


--
-- Data for Name: config_eqam; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.config_eqam (id, interface_pos, network_num) FROM stdin;
1	x2a	1
2	x2b	1
3	x2c	2
4	x2d	2
\.


--
-- Data for Name: config_hdmi; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.config_hdmi (id, interface_pos, format) FROM stdin;
\.


--
-- Data for Name: config_hls; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.config_hls (id, interface_pos, max_bitrate) FROM stdin;
1	h1	0
2	h2	0
3	h3	0
4	h4	0
5	h5	0
6	h6	0
7	h7	0
8	h8	0
9	h9	0
10	h10	0
11	h11	0
12	h12	0
13	h13	0
14	h14	0
15	h15	0
16	h16	0
17	h17	0
18	h18	0
19	h19	0
20	h20	0
21	h21	0
22	h22	0
23	h23	0
24	h24	0
25	h25	0
26	h26	0
27	h27	0
28	h28	0
29	h29	0
30	h30	0
31	h31	0
32	h32	0
33	h33	0
34	h34	0
35	h35	0
36	h36	0
37	h37	0
38	h38	0
39	h39	0
40	h40	0
\.


--
-- Data for Name: config_istr; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.config_istr (id, interface_pos, presentation_url) FROM stdin;
\.


--
-- Data for Name: config_sat; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.config_sat (id, interface_pos, lnb_type, freq, pol, symb, del, satno) FROM stdin;
\.


--
-- Data for Name: config_ter; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.config_ter (id, interface_pos, freq, bw, del) FROM stdin;
\.


--
-- Data for Name: config_webradio; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.config_webradio (id, interface_pos, gain, webradio_url) FROM stdin;
\.


--
-- Data for Name: forced_content; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.forced_content (id, enable, name, networks, volume, ts_filename, operation_mode, signal_type, signal_override, signal_status, com_status) FROM stdin;
1	f	Input 1	0	-1		0	0	0	0	f
2	f	Input 2	0	-1		0	0	0	0	f
3	f	Input 3	0	-1		0	0	0	0	f
4	f	Input 4	0	-1		0	0	0	0	f
\.


--
-- Data for Name: interfaces; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.interfaces (pos, name, type, active, scantime, multiband) FROM stdin;
h4	name?	hls2ip	f	\N	f
h5	name?	hls2ip	f	\N	f
h6	name?	hls2ip	f	\N	f
h7	name?	hls2ip	f	\N	f
h8	name?	hls2ip	f	\N	f
h9	name?	hls2ip	f	\N	f
h10	name?	hls2ip	f	\N	f
h11	name?	hls2ip	f	\N	f
h12	name?	hls2ip	f	\N	f
h13	name?	hls2ip	f	\N	f
h14	name?	hls2ip	f	\N	f
h15	name?	hls2ip	f	\N	f
h16	name?	hls2ip	f	\N	f
h17	name?	hls2ip	f	\N	f
h18	name?	hls2ip	f	\N	f
h19	name?	hls2ip	f	\N	f
h20	name?	hls2ip	f	\N	f
h21	name?	hls2ip	f	\N	f
h22	name?	hls2ip	f	\N	f
h23	name?	hls2ip	f	\N	f
h24	name?	hls2ip	f	\N	f
h25	name?	hls2ip	f	\N	f
h26	name?	hls2ip	f	\N	f
h27	name?	hls2ip	f	\N	f
h28	name?	hls2ip	f	\N	f
h29	name?	hls2ip	f	\N	f
h30	name?	hls2ip	f	\N	f
h31	name?	hls2ip	f	\N	f
h32	name?	hls2ip	f	\N	f
h33	name?	hls2ip	f	\N	f
h34	name?	hls2ip	f	\N	f
h35	name?	hls2ip	f	\N	f
h36	name?	hls2ip	f	\N	f
h37	name?	hls2ip	f	\N	f
h38	name?	hls2ip	f	\N	f
h39	name?	hls2ip	f	\N	f
h40	name?	hls2ip	f	\N	f
h1	CNN International	hls2ip	t	2026-02-03 16:26:35	f
h2	Kanal 5	hls2ip	t	\N	f
h3	SVT 1	hls2ip	t	\N	f
x1a	DSC-x1a	dsc	t	\N	f
x1b	DSC-x1b	dsc	f	\N	f
x2a	MOD-x2a	mod	t	\N	f
x2b	MOD-x2b	mod	f	\N	f
x2c	MOD-x2c	mod	f	\N	f
x2d	MOD-x2d	mod	f	\N	f
u1	JT UDP	dvbudp	t	\N	f
wr1	Lugna Favoriter	webradio	t	\N	f
\.


--
-- Data for Name: nv; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.nv (id, name, value) FROM stdin;
73	ixcloud_online	true
64	nw_hostname	209990
61	nw_gateway	192.168.0.1
62	nw_dns1	8.8.4.4
63	nw_dns2	
54	nw_eth0_mac	00:22:ab:80:7b:5e
50	nw_eth0_onboot	yes
51	nw_eth0_bootproto	static
52	nw_eth0_ipaddr	192.168.0.73
53	nw_eth0_netmask	255.255.255.0
59	nw_eth1_mac	00:22:ab:80:7b:5f
55	nw_eth1_onboot	yes
56	nw_eth1_bootproto	static
57	nw_eth1_ipaddr	172.16.0.1
58	nw_eth1_netmask	255.255.255.0
47	ui_serial	201024
48	ui_swversion	Ixanon GNU/Linux 4.3\n (1.24)
76	ixcloud_pingip	
68	ixcloud_validate_date	2026-02-09 10:23:13
72	ixcloud_beaconid	102
69	ixcloud_chanscan_url	http://ixcloud.ixanon.se/chanscan
70	ixcloud_chansave_url	
71	ixcloud_ping_ip	10.8.0.1
75	ixcloud_validate_message	Connected successfully
49	db_compatibility_version	8
60	nw_multicastdev	eth0
66	ixcloud_enable	true
67	ixcloud_validate_url	http://ixcloud.ixanon.se/he_validate
46	config_changed	true
37	bitrate_radio	300000
13	dvbc_net2_qam	QAM-256
9	dvbc_provider	Net
18	dvbc_net2_provider	Net2
24	hls_enable	false
25	hls_server_ip	127.0.0.1
26	hls_inport	7000
27	hls_outport	8000
28	hls_services	10
29	hls_playback_prefix	http://[ip]
30	portal_enable	false
31	portal_server_ip	127.0.0.1
32	portal_url	http://host/tvportal
65	ntp_enable	true
74	forced_content_enable	false
34	dsc_bitrate	65000000
2	dvbc_freq	330000000
33	dsc_services	8
45	remux_muxrate	10000000
3	dvbc_symb	6900000
36	bitrate_tvhd	10000000
42	remux_enable	false
8	dvbc_netname	Net
40	hls_ba_user	
5	dvbc_attenuation	10
38	hls_max_bitrate	10000000
10	dvbc_net2_enable	true
14	dvbc_net2_attenuation	0
20	ip_startaddr	239.1.1.1:10000
43	remux_audio_format	mp2
16	dvbc_net2_orgnetid	40961
39	hls_ba_enable	false
19	ip_enable	true
23	ip_tos	0
41	hls_ba_passwd	
15	dvbc_net2_netid	41002
44	remux_audio_offset	200
21	ip_netdev	eth0
22	ip_ttl	2
17	dvbc_net2_netname	Net2
7	dvbc_orgnetid	40961
1	dvbc_enable	true
12	dvbc_net2_symb	6900000
35	bitrate_tvsd	5000000
6	dvbc_netid	41001
4	dvbc_qam	QAM-256
11	dvbc_net2_freq	394000000
\.


--
-- Data for Name: routes; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.routes (id, service_key, output_name, lcn, dsc_pos, mod_pos, mod_pos_net2, out_sid, out_ip, epg_id, hls_enable) FROM stdin;
11	u1-1101	Seeded UDP One	1	None	x2a	x2c	101	239.1.1.81:10000		f
12	u1-1102	Seeded UDP Two HD	2	None	x2a	x2c	102	239.1.1.82:10000		f
13	u1-1103	Seeded UDP Radio	3	None	x2b	x2d	103	239.1.1.83:10000		f
14	wr1-2101	Seeded Webradio	4	None	x2b	x2d	104	239.1.1.61:10000		f
\.


--
-- Data for Name: services; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.services (id, interface_pos, name, key, sid, type, lang, all_langs, scrambled, enable, istr_url, istr_video, hls_url, webradio_url, epg_id) FROM stdin;
20	u1	Seeded UDP One	u1-1101	1101	TV_SD	eng	{eng}	f	t	\N	f	\N	\N	\N
21	u1	Seeded UDP Two HD	u1-1102	1102	TV_HD	eng	{eng,swe}	f	t	\N	f	\N	\N	\N
22	u1	Seeded UDP Radio	u1-1103	1103	RADIO	eng	{eng}	f	t	\N	f	\N	\N	\N
23	wr1	Seeded Webradio	wr1-2101	2101	RADIO	eng	{eng}	f	t	https://demo.local/audio/wr1/live.mp3	f	\N	https://demo.local/audio/wr1/live.mp3	\N
\.


--
-- Data for Name: user_sessions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_sessions (id, created, user_id, session_key) FROM stdin;
15	2026-02-25 09:50:07.939772	1	E6C3066D024845FB33D5030E75A00985
16	2026-02-28 16:34:38.478022	1	0A89D70061FC113A439B3F0019C7B147
17	2026-03-03 10:11:25.531741	1	B0CAA454F860FC0EDD04B735C320C18C
18	2026-03-03 11:52:56.024302	1	E55740940C596494436D656EA2934A08
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, name, password, edit, view) FROM stdin;
1	admin	password	f	f
\.


--
-- Name: config_dsc_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.config_dsc_id_seq', 2, true);


--
-- Name: config_dvbc_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.config_dvbc_id_seq', 1, false);


--
-- Name: config_dvbudp_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.config_dvbudp_id_seq', 1, false);


--
-- Name: config_eqam_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.config_eqam_id_seq', 4, true);


--
-- Name: config_hdmi_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.config_hdmi_id_seq', 1, false);


--
-- Name: config_hls_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.config_hls_id_seq', 40, true);


--
-- Name: config_istr_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.config_istr_id_seq', 1, false);


--
-- Name: config_sat_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.config_sat_id_seq', 1, false);


--
-- Name: config_ter_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.config_ter_id_seq', 1, false);


--
-- Name: config_webradio_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.config_webradio_id_seq', 1, false);


--
-- Name: nv_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.nv_id_seq', 76, true);


--
-- Name: routes_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.routes_id_seq', 14, true);


--
-- Name: services_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.services_id_seq', 23, true);


--
-- Name: user_sessions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.user_sessions_id_seq', 18, true);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.users_id_seq', 1, true);


--
-- Name: config_dsc config_dsc_interface_pos_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_dsc
    ADD CONSTRAINT config_dsc_interface_pos_key UNIQUE (interface_pos);


--
-- Name: config_dsc config_dsc_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_dsc
    ADD CONSTRAINT config_dsc_pkey PRIMARY KEY (id);


--
-- Name: config_dvbc config_dvbc_interface_pos_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_dvbc
    ADD CONSTRAINT config_dvbc_interface_pos_key UNIQUE (interface_pos);


--
-- Name: config_dvbc config_dvbc_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_dvbc
    ADD CONSTRAINT config_dvbc_pkey PRIMARY KEY (id);


--
-- Name: config_dvbudp config_dvbudp_interface_pos_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_dvbudp
    ADD CONSTRAINT config_dvbudp_interface_pos_key UNIQUE (interface_pos);


--
-- Name: config_dvbudp config_dvbudp_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_dvbudp
    ADD CONSTRAINT config_dvbudp_pkey PRIMARY KEY (id);


--
-- Name: config_emm config_emm_interface_pos_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_emm
    ADD CONSTRAINT config_emm_interface_pos_key UNIQUE (interface_pos);


--
-- Name: config_eqam config_eqam_interface_pos_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_eqam
    ADD CONSTRAINT config_eqam_interface_pos_key UNIQUE (interface_pos);


--
-- Name: config_eqam config_eqam_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_eqam
    ADD CONSTRAINT config_eqam_pkey PRIMARY KEY (id);


--
-- Name: config_hdmi config_hdmi_interface_pos_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_hdmi
    ADD CONSTRAINT config_hdmi_interface_pos_key UNIQUE (interface_pos);


--
-- Name: config_hdmi config_hdmi_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_hdmi
    ADD CONSTRAINT config_hdmi_pkey PRIMARY KEY (id);


--
-- Name: config_hls config_hls_interface_pos_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_hls
    ADD CONSTRAINT config_hls_interface_pos_key UNIQUE (interface_pos);


--
-- Name: config_hls config_hls_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_hls
    ADD CONSTRAINT config_hls_pkey PRIMARY KEY (id);


--
-- Name: config_istr config_istr_interface_pos_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_istr
    ADD CONSTRAINT config_istr_interface_pos_key UNIQUE (interface_pos);


--
-- Name: config_istr config_istr_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_istr
    ADD CONSTRAINT config_istr_pkey PRIMARY KEY (id);


--
-- Name: config_sat config_sat_interface_pos_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_sat
    ADD CONSTRAINT config_sat_interface_pos_key UNIQUE (interface_pos);


--
-- Name: config_sat config_sat_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_sat
    ADD CONSTRAINT config_sat_pkey PRIMARY KEY (id);


--
-- Name: config_ter config_ter_interface_pos_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_ter
    ADD CONSTRAINT config_ter_interface_pos_key UNIQUE (interface_pos);


--
-- Name: config_ter config_ter_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_ter
    ADD CONSTRAINT config_ter_pkey PRIMARY KEY (id);


--
-- Name: config_webradio config_webradio_interface_pos_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_webradio
    ADD CONSTRAINT config_webradio_interface_pos_key UNIQUE (interface_pos);


--
-- Name: config_webradio config_webradio_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_webradio
    ADD CONSTRAINT config_webradio_pkey PRIMARY KEY (id);


--
-- Name: forced_content forced_content_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.forced_content
    ADD CONSTRAINT forced_content_id_key UNIQUE (id);


--
-- Name: interfaces interfaces_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.interfaces
    ADD CONSTRAINT interfaces_pkey PRIMARY KEY (pos);


--
-- Name: nv nv_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.nv
    ADD CONSTRAINT nv_pkey PRIMARY KEY (id);


--
-- Name: routes routes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.routes
    ADD CONSTRAINT routes_pkey PRIMARY KEY (id);


--
-- Name: services services_key_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.services
    ADD CONSTRAINT services_key_key UNIQUE (key);


--
-- Name: services services_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.services
    ADD CONSTRAINT services_pkey PRIMARY KEY (id);


--
-- Name: user_sessions user_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_sessions
    ADD CONSTRAINT user_sessions_pkey PRIMARY KEY (id);


--
-- Name: users users_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_name_key UNIQUE (name);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- PostgreSQL database dump complete
--

