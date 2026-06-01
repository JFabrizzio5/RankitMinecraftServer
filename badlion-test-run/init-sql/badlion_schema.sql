-- PostgreSQL Schema for Badlion.net Core Plugins

-- ==========================================
-- Gberry Database Tables
-- ==========================================

CREATE TABLE user_data (
   uuid character varying(36) NOT NULL,
   currency integer NOT NULL DEFAULT 0,
   cosmetics json NOT NULL DEFAULT '{}',
   player_visibility boolean NOT NULL DEFAULT 'true',
   cases json NOT NULL DEFAULT '{}',
   sg_settings json NOT NULL DEFAULT '{}',
   disguise_settings json NOT NULL DEFAULT '{}',
   chat_settings json NOT NULL DEFAULT '{}',
   arena_settings json NOT NULL DEFAULT '{}',
   stat_resets json NOT NULL DEFAULT '{}',
   banned_stats json NOT NULL DEFAULT '{}',
   false_ban BOOLEAN NOT NULL DEFAULT 'false',
   lobby_flight BOOLEAN NOT NULL DEFAULT 'false'
);

ALTER TABLE ONLY user_data ADD CONSTRAINT "Noneuser_data_uuid_pkey" PRIMARY KEY (uuid);

CREATE TABLE global_settings (
   setting character varying(36) NOT NULL,
   val character varying(36) NOT NULL
);

ALTER TABLE ONLY global_settings ADD CONSTRAINT "Noneglobal_settings" PRIMARY KEY (setting);

CREATE TABLE gchat_filters (
   regex character varying(128) NOT NULL,
   punishment_length character varying(5) NOT NULL,
   punishment_type character varying(1) NOT NULL,
   reason character varying(128) NOT NULL
);

ALTER TABLE ONLY gchat_filters ADD CONSTRAINT "Nonegchat_filters" PRIMARY KEY (regex);

CREATE TABLE badlion_performance (
   server_name character varying(32) NOT NULL,
   ts timestamp with time zone NOT NULL,
   traces text NOT NULL
);

CREATE INDEX "Nonebadlion_performance_server_name" ON badlion_performance USING btree (server_name);
CREATE INDEX "Nonebadlion_performance_ts" ON badlion_performance USING btree (ts);

CREATE TABLE player_command_logs (
  uuid character varying(36) NOT NULL,
  log_time timestamp without time zone NOT NULL,
  server_name character varying(16) NOT NULL,
  command TEXT NOT NULL
);

CREATE INDEX "Noneplayer_command_logs_uuid" ON player_command_logs USING btree (uuid);
CREATE INDEX "Noneplayer_command_logs_log_time" ON player_command_logs USING btree (log_time);

CREATE TABLE server_reboot_times (
  server_name character varying(16) NOT NULL,
  reboot_time character varying(5) NOT NULL,
  reboot_with_players BOOLEAN NOT NULL
);

ALTER TABLE ONLY server_reboot_times
ADD CONSTRAINT "Noneserver_reboot_times_server_name_reboot_time_pkey" PRIMARY KEY (server_name, reboot_time);


-- ==========================================
-- BanManager Database Tables (Converted from MySQL to PostgreSQL)
-- ==========================================

CREATE TABLE ban_list (
  banned_uuid VARCHAR(64) NOT NULL PRIMARY KEY,
  banner_uuid VARCHAR(64) NOT NULL,
  banned_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  unban_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  server VARCHAR(16) NOT NULL,
  reason VARCHAR(255) NOT NULL
);

CREATE TABLE ban_records (
  banned_uuid VARCHAR(64) NOT NULL,
  banner_uuid VARCHAR(64) NOT NULL,
  banned_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  unban_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  server VARCHAR(16) NOT NULL,
  reason VARCHAR(255) NOT NULL,
  unbanner_uuid VARCHAR(64) NOT NULL,
  unbanned_time TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE kick_list (
  kicked_uuid VARCHAR(64) NOT NULL,
  kicker_uuid VARCHAR(64) NOT NULL,
  reason VARCHAR(255) NOT NULL,
  time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  server VARCHAR(15) NULL DEFAULT NULL
);

CREATE TABLE mute_list (
  muted_uuid VARCHAR(64) NOT NULL PRIMARY KEY,
  muter_uuid VARCHAR(64) NOT NULL,
  muted_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  unmute_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  server VARCHAR(16) NOT NULL,
  reason VARCHAR(255) NOT NULL
);

CREATE TABLE mute_records (
  muted_uuid VARCHAR(64) NOT NULL,
  muter_uuid VARCHAR(64) NOT NULL,
  muted_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  unmute_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  server VARCHAR(16) NOT NULL,
  reason VARCHAR(255) NOT NULL,
  unmuter_uuid VARCHAR(64) NOT NULL,
  unmuted_time TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE SEQUENCE punishments_punishment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE punishments (
    punishment_id integer DEFAULT nextval('punishments_punishment_id_seq'::regclass) NOT NULL,
    punished_uuid character varying(36) NOT NULL,
    punisher_uuid character varying(36) NOT NULL,
    punishment_time timestamp with time zone NOT NULL,
    unpunish_time timestamp with time zone NOT NULL,
    unpunisher_uuid character varying(36) NULL,
    server character varying(20) NOT NULL,
    reason character varying(255) NOT NULL,
    type integer NOT NULL,
    ip bigint NOT NULL default 0,
    false_punishment boolean default FALSE,
    un_appealable boolean default FALSE
);

ALTER TABLE ONLY punishments
    ADD CONSTRAINT "Nonepunishments_punishment_id" PRIMARY KEY (punishment_id);
    
CREATE INDEX "Nonepunishments_punished_uuid" ON punishments USING btree (punished_uuid);
CREATE INDEX "Nonepunishments_punisher_uuid" ON punishments USING btree (punisher_uuid);
CREATE INDEX "Nonepunishments_unpunisher_uuid" ON punishments USING btree (unpunisher_uuid);
CREATE INDEX "Nonepunishments_punishment_time" ON punishments USING btree (punishment_time);
CREATE INDEX "Nonepunishments_type" ON punishments USING btree (type);
CREATE INDEX "Nonepunishments_ip" ON punishments USING btree (ip);
CREATE INDEX "Nonepunishments_unpunish_time" ON punishments USING btree (unpunish_time);

-- ==========================================
-- GPermissions Database Tables (na_lobby_1 prefix)
-- ==========================================

CREATE TABLE na_lobby_1_gperms_groups (
    group_name character varying(55) NOT NULL,
    prefix character varying(55) NOT NULL
);

ALTER TABLE ONLY na_lobby_1_gperms_groups
    ADD CONSTRAINT "Nonena_lobby_1_gperms_groups_group_name_pkey" PRIMARY KEY (group_name);

CREATE TABLE na_lobby_1_gperms_group_permissions (
    group_name character varying(55) NOT NULL,
    permission_name character varying(55) NOT NULL
);

ALTER TABLE ONLY na_lobby_1_gperms_group_permissions
    ADD CONSTRAINT "Nonena_lobby_1_gperms_group_permissions_group_name_permission_name_p" PRIMARY KEY (group_name, permission_name);

CREATE TABLE na_lobby_1_gperms_users (
    uuid character varying(64) NOT NULL,
    "group" character varying(55) NOT NULL,
    prefix character varying(55) NOT NULL
);

ALTER TABLE ONLY na_lobby_1_gperms_users
    ADD CONSTRAINT "Nonena_lobby_1_gperms_users_uuid_pkey" PRIMARY KEY (uuid);

CREATE TABLE na_lobby_1_gperms_user_permissions (
    uuid character varying(64) NOT NULL,
    permission_name character varying(55) NOT NULL
);

ALTER TABLE ONLY na_lobby_1_gperms_user_permissions
    ADD CONSTRAINT "Nonena_lobby_1_gperms_user_permissions_uuid_permission_name_pkey" PRIMARY KEY (uuid, permission_name);

CREATE TABLE na_lobby_1_gperms_donators (
    uuid character varying(64) NOT NULL
);

CREATE TABLE na_lobby_1_gperms_user_subgroups (
    uuid character varying(36) NOT NULL,
    group_name character varying(55) NOT NULL
);

ALTER TABLE ONLY na_lobby_1_gperms_user_subgroups
    ADD CONSTRAINT "Nonena_lobby_1_gperms_user_subgroups_uuid_group_name_pkey" PRIMARY KEY (uuid, group_name);

ALTER TABLE ONLY na_lobby_1_gperms_donators
    ADD CONSTRAINT "Nonena_lobby_1_gperms_donators_uuid_pkey" PRIMARY KEY (uuid);

CREATE TABLE na_lobby_1_gperms_famous (
    uuid character varying(64) NOT NULL
);

ALTER TABLE ONLY na_lobby_1_gperms_famous
    ADD CONSTRAINT "Nonena_lobby_1_gperms_famous_uuid_pkey" PRIMARY KEY (uuid);

CREATE TABLE na_lobby_1_gperms_youtubers (
    uuid character varying(64) NOT NULL
);

ALTER TABLE ONLY na_lobby_1_gperms_youtubers
    ADD CONSTRAINT "Nonena_lobby_1_gperms_youtubers_uuid_pkey" PRIMARY KEY (uuid);

CREATE TABLE na_lobby_1_gperms_twitch (
    uuid character varying(64) NOT NULL
);

ALTER TABLE ONLY na_lobby_1_gperms_twitch
    ADD CONSTRAINT "Nonena_lobby_1_gperms_twitch_uuid_pkey" PRIMARY KEY (uuid);


-- ==========================================
-- Pre-populate Roles and Permissions
-- ==========================================

-- Insert groups
INSERT INTO na_lobby_1_gperms_groups (group_name, prefix) VALUES ('default', '§7');
INSERT INTO na_lobby_1_gperms_groups (group_name, prefix) VALUES ('owner', '§4[Owner] §4');

-- Insert owner permissions (Universal Permission)
INSERT INTO na_lobby_1_gperms_group_permissions (group_name, permission_name) VALUES ('owner', '*');

-- Assign jos5_yt as Owner
INSERT INTO na_lobby_1_gperms_users (uuid, "group", prefix) VALUES ('d65f8845-7f80-385c-87cf-6a26e11aa5b7', 'owner', '§4[Owner] §4');

-- Register jos5_yt as Donator
INSERT INTO na_lobby_1_gperms_donators (uuid) VALUES ('d65f8845-7f80-385c-87cf-6a26e11aa5b7');

-- ==========================================
-- Disguise Database Tables
-- ==========================================
CREATE TABLE disguise_history (
  uuid character varying(36) NOT NULL,
  disguise_name character varying(16) NOT NULL,
  disguise_time TIMESTAMP without time zone NOT NULL,
  undisguise_time TIMESTAMP without time zone
);

CREATE INDEX "Nonedisguise_history_uuid" ON disguise_history USING btree (uuid);

CREATE TABLE disguise_names (
  disguise_name character varying(16) NOT NULL,
  in_use BOOLEAN NOT NULL
);

CREATE TABLE disguise_skins (
  texture TEXT NOT NULL,
  signature TEXT NOT NULL
);

-- ==========================================
-- MPG Database Tables
-- ==========================================
CREATE TABLE mpg_kits (
    uuid character varying(36) NOT NULL,
    gamemode character varying(10) NOT NULL,
    type character varying(10) NOT NULL,
    preview_item integer NOT NULL,
    items bytea NOT NULL,
    armor bytea NOT NULL,
    kit_number integer NOT NULL,
    name character varying(25) NOT NULL
);

ALTER TABLE ONLY mpg_kits ADD CONSTRAINT "Nonempg_kits_pkey" PRIMARY KEY (uuid, gamemode, kit_number);
CREATE INDEX "Nonempg_kits_uuid" ON mpg_kits USING btree (uuid);

CREATE TABLE clan_duel_stats (
  clan_id integer NOT NULL,
  game_type character varying(16) NOT NULL,
  wins integer DEFAULT 0,
  losses integer DEFAULT 0
);

CREATE INDEX "Noneclan_duel_stats_clan_id" ON clan_duel_stats USING btree(clan_id);
CREATE INDEX "Noneclan_duel_stats_game_type" ON clan_duel_stats USING btree(game_type);

CREATE TABLE clan_duel_records (
  time timestamp with time zone NOT NULL,
  clan_id integer NOT NULL,
  other_clan_id integer NOT NULL,
  game_type character varying(16) NOT NULL,
  match_id character varying(36) NOT NULL,
  win BOOLEAN NOT NULL
);

CREATE INDEX "Noneclan_duel_records_clan_id" ON clan_duel_records USING btree(clan_id);
CREATE INDEX "Noneclan_duel_records_other_clan_id" ON clan_duel_records USING btree(other_clan_id);
CREATE INDEX "Noneclan_duel_records_game_type" ON clan_duel_records USING btree(game_type);

-- ==========================================
-- WorldRotator Database Tables (PostgreSQL Syntax)
-- ==========================================
CREATE TABLE IF NOT EXISTS arenas (
	arena_name VARCHAR(55) NOT NULL,
	warp_1 VARCHAR(55) NOT NULL,
	warp_2 VARCHAR(55) NOT NULL,
	PRIMARY KEY (arena_name)
);

CREATE TABLE IF NOT EXISTS ladders (
	ladder_id SERIAL,
	ladder_name VARCHAR(55) NOT NULL,
	PRIMARY KEY (ladder_id)
);

CREATE TABLE IF NOT EXISTS ladder_ratings (
	lid INT NOT NULL,
	username VARCHAR (55) NOT NULL,
	rating INT NOT NULL,
	wins INT NOT NULL,
	losses INT NOT NULL,
	PRIMARY KEY (lid, username)
);

CREATE TABLE IF NOT EXISTS user_limits (
	username VARCHAR (55) NOT NULL,
	amount_donated INT NOT NULL,
	ranked_limit_per_day INT NOT NULL,
	expiration_date DATE NOT NULL,
	PRIMARY KEY (username)
);

CREATE TABLE IF NOT EXISTS user_num_of_ranked (
	username VARCHAR (55) NOT NULL,
	num_of_matches INT NOT NULL,
	day DATE NOT NULL,
	PRIMARY KEY (username, day)
);

CREATE TABLE IF NOT EXISTS sg_user_num_of_ranked (
  username VARCHAR (55) NOT NULL,
  num_of_matches INT NOT NULL,
  day DATE NOT NULL,
  PRIMARY KEY (username, day)
);

-- ==========================================
-- MiniStats Database Tables (UHC & MPG Prefixes)
-- ==========================================

-- UHC MiniStats
CREATE TABLE uhc_ministats (
    uuid character varying(36) NOT NULL,
    kills integer NOT NULL,
    deaths integer NOT NULL,
    wins integer NOT NULL,
    losses integer NOT NULL,
    time_played bigint NOT NULL,
    damage_dealt double precision NOT NULL,
    damage_taken double precision NOT NULL,
    highest_kill_streak integer NOT NULL,
    sword_hits integer NOT NULL,
    sword_swings integer NOT NULL,
    sword_blocks integer NOT NULL,
    bow_punches integer NOT NULL,
    arrows_shot integer NOT NULL,
    arrows_hit integer NOT NULL,
    sword_accuracy double precision NOT NULL DEFAULT '0',
    arrow_accuracy double precision NOT NULL DEFAULT '0',
    kdr double precision NOT NULL DEFAULT '0',
    levels integer NOT NULL DEFAULT 0,
    hearts_healed integer NOT NULL DEFAULT 0,
    horses_tamed integer NOT NULL DEFAULT 0,
    fall_damage double precision NOT NULL DEFAULT 0,
    absorption_hearts integer NOT NULL DEFAULT 0,
    golden_heads integer NOT NULL DEFAULT 0,
    golden_apples integer NOT NULL DEFAULT 0,
    nether_portals integer NOT NULL DEFAULT 0,
    end_portals integer NOT NULL DEFAULT 0,
    blocks_broken json NOT NULL DEFAULT '{}',
    animal_mobs json NOT NULL DEFAULT '{}',
    potions json NOT NULL DEFAULT '{}'
);

ALTER TABLE ONLY uhc_ministats ADD CONSTRAINT "Noneuhc_ministats_uuid_pkey" PRIMARY KEY (uuid);
CREATE INDEX "Noneuhc_ministats_kills" ON uhc_ministats USING btree (kills);
CREATE INDEX "Noneuhc_ministats_deaths" ON uhc_ministats USING btree (deaths);
CREATE INDEX "Noneuhc_ministats_wins" ON uhc_ministats USING btree (wins);
CREATE INDEX "Noneuhc_ministats_losses" ON uhc_ministats USING btree (losses);
CREATE INDEX "Noneuhc_ministats_levels" ON uhc_ministats USING btree (levels);
CREATE INDEX "Noneuhc_ministats_hearts_healed" ON uhc_ministats USING btree (hearts_healed);
CREATE INDEX "Noneuhc_ministats_horses_tamed" ON uhc_ministats USING btree (horses_tamed);
CREATE INDEX "Noneuhc_ministats_fall_damage" ON uhc_ministats USING btree (fall_damage);
CREATE INDEX "Noneuhc_ministats_absorption_hearts" ON uhc_ministats USING btree (absorption_hearts);
CREATE INDEX "Noneuhc_ministats_golden_heads" ON uhc_ministats USING btree (golden_heads);
CREATE INDEX "Noneuhc_ministats_golden_apples" ON uhc_ministats USING btree (golden_apples);
CREATE INDEX "Noneuhc_ministats_nether_portals" ON uhc_ministats USING btree (nether_portals);
CREATE INDEX "Noneuhc_ministats_end_portals" ON uhc_ministats USING btree (end_portals);

CREATE TABLE uhc_ministats_maps (
    uuid character varying(36) NOT NULL,
    map_name character varying(36) NOT NULL,
    wins integer NOT NULL,
    losses integer NOT NULL
);

CREATE INDEX "Noneuhc_ministats_maps_uuid" ON uhc_ministats_maps USING btree (uuid);
CREATE INDEX "Noneuhc_ministats_maps_map_name" ON uhc_ministats_maps USING btree (map_name);

-- MPG MiniStats
CREATE TABLE mpg_ministats (
    uuid character varying(36) NOT NULL,
    kills integer NOT NULL,
    deaths integer NOT NULL,
    wins integer NOT NULL,
    losses integer NOT NULL,
    time_played bigint NOT NULL,
    damage_dealt double precision NOT NULL,
    damage_taken double precision NOT NULL,
    highest_kill_streak integer NOT NULL,
    sword_hits integer NOT NULL,
    sword_swings integer NOT NULL,
    sword_blocks integer NOT NULL,
    bow_punches integer NOT NULL,
    arrows_shot integer NOT NULL,
    arrows_hit integer NOT NULL,
    sword_accuracy double precision NOT NULL DEFAULT '0',
    arrow_accuracy double precision NOT NULL DEFAULT '0',
    kdr double precision NOT NULL DEFAULT '0'
);

ALTER TABLE ONLY mpg_ministats ADD CONSTRAINT "Nonempg_ministats_uuid_pkey" PRIMARY KEY (uuid);
CREATE INDEX "Nonempg_ministats_kills" ON mpg_ministats USING btree (kills);
CREATE INDEX "Nonempg_ministats_deaths" ON mpg_ministats USING btree (deaths);
CREATE INDEX "Nonempg_ministats_wins" ON mpg_ministats USING btree (wins);
CREATE INDEX "Nonempg_ministats_losses" ON mpg_ministats USING btree (losses);

CREATE TABLE mpg_ministats_maps (
    uuid character varying(36) NOT NULL,
    map_name character varying(36) NOT NULL,
    wins integer NOT NULL,
    losses integer NOT NULL
);

CREATE INDEX "Nonempg_ministats_maps_uuid" ON mpg_ministats_maps USING btree (uuid);
CREATE INDEX "Nonempg_ministats_maps_map_name" ON mpg_ministats_maps USING btree (map_name);

-- ==========================================
-- UHC Match & Stat Reset Tables
-- ==========================================
CREATE SEQUENCE uhc_match_times_uhc_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE uhc_match_times (
    uhc_id integer DEFAULT nextval('uhc_match_times_uhc_id_seq'::regclass) NOT NULL,
    uhc_time timestamp without time zone NOT NULL,
    uhc_uid integer NOT NULL,
    uhc_hosts text NOT NULL,
    uhc_guests text NOT NULL,
    uhc_max_players integer NOT NULL,
    uhc_game_mode text NOT NULL,
    region character varying(1) NOT NULL
);

ALTER TABLE ONLY uhc_match_times
    ADD CONSTRAINT "Noneuhc_match_times_uhc_id_pkey" PRIMARY KEY (uhc_id);

CREATE SEQUENCE uhc_stat_resets_reset_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE uhc_stat_resets (
    reset_id integer DEFAULT nextval('uhc_stat_resets_reset_id_seq'::regclass) NOT NULL,
    uuid character varying(36) NOT NULL
);

ALTER TABLE ONLY uhc_stat_resets
    ADD CONSTRAINT "Noneuhc_stat_resets_reset_id_pkey" PRIMARY KEY (reset_id);

-- Extra permission mappings for jos5_yt (just in case they don't resolve *)
INSERT INTO na_lobby_1_gperms_user_permissions (uuid, permission_name) VALUES ('d65f8845-7f80-385c-87cf-6a26e11aa5b7', 'badlion.tournament');
INSERT INTO na_lobby_1_gperms_user_permissions (uuid, permission_name) VALUES ('d65f8845-7f80-385c-87cf-6a26e11aa5b7', 'badlion.uhcsrhost');
INSERT INTO na_lobby_1_gperms_user_permissions (uuid, permission_name) VALUES ('d65f8845-7f80-385c-87cf-6a26e11aa5b7', 'badlion.uhctrial');
INSERT INTO na_lobby_1_gperms_user_permissions (uuid, permission_name) VALUES ('d65f8845-7f80-385c-87cf-6a26e11aa5b7', 'badlion.mapcreator');
INSERT INTO na_lobby_1_gperms_user_permissions (uuid, permission_name) VALUES ('d65f8845-7f80-385c-87cf-6a26e11aa5b7', 'badlion.admin');



