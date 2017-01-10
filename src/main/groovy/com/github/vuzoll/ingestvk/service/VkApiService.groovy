package com.github.vuzoll.ingestvk.service

import com.vk.api.sdk.client.Lang
import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.client.actors.UserActor
import com.vk.api.sdk.exceptions.ApiAuthValidationException
import com.vk.api.sdk.exceptions.ApiUserDeletedException
import com.vk.api.sdk.httpclient.HttpTransportClient
import com.vk.api.sdk.objects.users.UserFull
import com.vk.api.sdk.queries.users.UserField
import groovy.util.logging.Slf4j
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service

@Scope('singleton')
@Service
@Slf4j
class VkApiService {

    static final int MAX_REQUEST_SIZE = 1000

    static final Integer VK_USER_ID = System.getenv('INGEST_VK_USER_ID') ? Integer.parseInt(System.getenv('INGEST_VK_USER_ID')) : null
    static final String VK_ACCESS_TOKEN = System.getenv('INGEST_VK_ACCESS_TOKEN')

    static final long VK_API_REQUEST_DELAY = 350

    VkApiClient vk = new VkApiClient(HttpTransportClient.getInstance())

    UserFull ingestVkProfileById(Integer id) {
        ingestVkProfilesById([ id ]).first()
    }

    Collection<UserFull> ingestVkProfilesById(Collection<Integer> ids) {
        if (ids.size() > 1000) {
            log.warn "Request for ${ids.size()} exceed max limit $MAX_REQUEST_SIZE"
            throw new IllegalStateException("Request for ${ids.size()} exceed max limit $MAX_REQUEST_SIZE")
        }

        log.debug "Ingesting ${ids.size()} vk profiles ids=$ids..."
        Thread.sleep(VK_API_REQUEST_DELAY)

        doIngestVkProfilesById(ids)
    }

    Collection<Integer> getFriendsIds(Integer id) {
        log.debug "Getting friend list of profile id=$id..."
        Thread.sleep(VK_API_REQUEST_DELAY)

        doGetFriendsIds(id)
    }

    private Collection<UserFull> doIngestVkProfilesById(Collection<Integer> ids) {
        try {
            def vkRequest
            if (VK_USER_ID && VK_ACCESS_TOKEN) {
                vkRequest = vk.users().get(new UserActor(VK_USER_ID, VK_ACCESS_TOKEN))
            } else {
                vkRequest = vk.users().get()
            }

            vkRequest.userIds(ids.collect({ it.toString() }))
                    .fields(
                    UserField.ABOUT,     UserField.ACTIVITIES, UserField.BDATE,        UserField.BOOKS,
                    UserField.CAREER,    UserField.CITY,       UserField.CONNECTIONS,  UserField.CONTACTS,
                    UserField.COUNTRY,   UserField.DOMAIN,     UserField.EDUCATION,    UserField.GAMES,
                    UserField.HOME_TOWN, UserField.INTERESTS,  UserField.LAST_SEEN,    UserField.MILITARY,
                    UserField.MOVIES,    UserField.MUSIC,      UserField.OCCUPATION,   UserField.PERSONAL,
                    UserField.QUOTES,    UserField.RELATIVES,  UserField.RELATION,     UserField.SCHOOLS,
                    UserField.SEX,       UserField.TV,         UserField.UNIVERSITIES, UserField.VERIFIED
            ).lang(Lang.UA).execute()
        } catch (ApiAuthValidationException e) {
            throw new RuntimeException("vk validation required - visit $e.redirectUri", e)
        }
    }

    Collection<Integer> doGetFriendsIds(Integer id) {
        try {
            def vkRequest
            if (VK_USER_ID && VK_ACCESS_TOKEN) {
                vkRequest = vk.friends().get(new UserActor(VK_USER_ID, VK_ACCESS_TOKEN))
            } else {
                vkRequest = vk.friends().get()
            }

            return vkRequest
                    .userId(id)
                    .execute()
                    .items
        } catch (ApiAuthValidationException e) {
            log.error("vk validation required - visit $e.redirectUri", e)
            throw new RuntimeException("vk validation required - visit $e.redirectUri", e)
        } catch (ApiUserDeletedException e) {
            log.debug("User with id=$id was deactivated", e)
            return []
        }
    }
}
