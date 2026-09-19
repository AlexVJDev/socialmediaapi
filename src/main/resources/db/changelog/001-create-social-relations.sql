--liquibase formatted sql

--changeset student:001-create-social-relations
CREATE TABLE friend_requests (
                                 id BIGSERIAL PRIMARY KEY,
                                 requester_id UUID NOT NULL,
                                 recipient_id UUID NOT NULL,
                                 status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 CONSTRAINT uq_friend_request UNIQUE (requester_id, recipient_id),
                                 CONSTRAINT chk_friend_request_users CHECK (requester_id <> recipient_id),
                                 CONSTRAINT chk_friend_request_status
                                     CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED'))
);

CREATE TABLE subscriptions (
                               subscriber_id UUID NOT NULL,
                               target_id UUID NOT NULL,
                               created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               PRIMARY KEY (subscriber_id, target_id),
                               CONSTRAINT chk_subscription_users CHECK (subscriber_id <> target_id)
);

CREATE TABLE friendships (
                             user_id UUID NOT NULL,
                             friend_id UUID NOT NULL,
                             created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             PRIMARY KEY (user_id, friend_id),
                             CONSTRAINT chk_friendship_users CHECK (user_id <> friend_id)
);

CREATE TABLE notifications (
                               id BIGSERIAL PRIMARY KEY,
                               user_id UUID NOT NULL,
                               type VARCHAR(50) NOT NULL,
                               message TEXT NOT NULL,
                               created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               read_at TIMESTAMPTZ
);

--rollback DROP TABLE notifications;
--rollback DROP TABLE friendships;
--rollback DROP TABLE subscriptions;
--rollback DROP TABLE friend_requests;
