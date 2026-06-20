package com.example.demo.sentiment_analysis.posts.controller;

import com.example.demo.sentiment_analysis.config.TestSecurityConfig;
import com.example.demo.sentiment_analysis.exception.PostsNotFoundException;
import com.example.demo.sentiment_analysis.posts.enumeration.TypeOfAccess;
import com.example.demo.sentiment_analysis.posts.model.Posts;
import com.example.demo.sentiment_analysis.posts.post_request_dto.PostDto;
import com.example.demo.sentiment_analysis.posts.posts_response.PostDetailDto;
import com.example.demo.sentiment_analysis.posts.posts_response.PostResponseDto;
import com.example.demo.sentiment_analysis.posts.service.LogicOfPosts;
import com.example.demo.sentiment_analysis.slice_response_dto.PaginatedResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FrontControllerOfPosts.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class FrontControllerOfPostsTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean  private LogicOfPosts logicOfPosts;

    private ObjectId postId;
    private Posts post;

    @BeforeEach
    void setUp() {
        postId = new ObjectId();

        post = new Posts();
        post.setId(postId);
        post.setTitle("My Post");
        post.setContent("Some content here");
        post.setType(TypeOfAccess.PUBLIC);
        post.setCreateAt(LocalDateTime.now());

        User principal = new User("alice@test.com", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // -----------------------------------------------------------------------
    // GET /api/posts
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/posts → 200 OK with posts list and success message")
    void getMyPosts_returns200() throws Exception {
        PostResponseDto dto = new PostResponseDto(
                postId.toHexString(), "My Post", "Content",
                "alice@test.com", LocalDateTime.now(), 0L, 0L);

        PaginatedResponse<PostResponseDto> paged = new PaginatedResponse<>();
        paged.setContent(List.of(dto));
        paged.setPageNumber(0);
        paged.setPageSize(10);

        when(logicOfPosts.getPostsByUserEmail(eq("alice@test.com"), any(Pageable.class)))
                .thenReturn(paged);

        mockMvc.perform(get("/api/posts").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Posts fetched successfully"))
                .andExpect(jsonPath("$.data.content[0].title").value("My Post"));
    }

    @Test
    @DisplayName("GET /api/posts → uses authenticated user's email")
    void getMyPosts_usesAuthenticatedEmail() throws Exception {
        PaginatedResponse<PostResponseDto> paged = new PaginatedResponse<>();
        paged.setContent(List.of());
        when(logicOfPosts.getPostsByUserEmail(eq("alice@test.com"), any())).thenReturn(paged);

        mockMvc.perform(get("/api/posts")).andExpect(status().isOk());

        verify(logicOfPosts).getPostsByUserEmail(eq("alice@test.com"), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/posts → 200 OK with empty content when no posts")
    void getMyPosts_emptyList_returns200() throws Exception {
        PaginatedResponse<PostResponseDto> paged = new PaginatedResponse<>();
        paged.setContent(List.of());
        when(logicOfPosts.getPostsByUserEmail(any(), any())).thenReturn(paged);

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    // -----------------------------------------------------------------------
    // POST /api/posts
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/posts → 201 CREATED with new post")
    void createPost_returns201() throws Exception {
        when(logicOfPosts.createPostForUser(any(PostDto.class), eq("alice@test.com")))
                .thenReturn(post);

        PostDto dto = new PostDto(null, "My Post", "Some content here", TypeOfAccess.PUBLIC);
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("My Post"));
    }

    @Test
    @DisplayName("POST /api/posts → 400 BAD_REQUEST when title is blank")
    void createPost_blankTitle_returns400() throws Exception {
        PostDto invalid = new PostDto(null, "", "Content", TypeOfAccess.PUBLIC);
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/posts → 400 BAD_REQUEST when content is blank")
    void createPost_blankContent_returns400() throws Exception {
        PostDto invalid = new PostDto(null, "Title", "", TypeOfAccess.PUBLIC);
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/posts → service called with authenticated email")
    void createPost_usesAuthenticatedEmail() throws Exception {
        when(logicOfPosts.createPostForUser(any(), eq("alice@test.com"))).thenReturn(post);

        PostDto dto = new PostDto(null, "My Post", "Some content here", TypeOfAccess.PUBLIC);
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        verify(logicOfPosts).createPostForUser(any(PostDto.class), eq("alice@test.com"));
    }

    // -----------------------------------------------------------------------
    // DELETE /api/posts/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("DELETE /api/posts/{id} → 204 NO_CONTENT on success")
    void deletePost_returns204() throws Exception {
        doNothing().when(logicOfPosts).removePost(eq(postId), eq("alice@test.com"));

        mockMvc.perform(delete("/api/posts/{id}", postId.toHexString()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/posts/{id} → service called with correct id and email")
    void deletePost_passesCorrectArgs() throws Exception {
        doNothing().when(logicOfPosts).removePost(any(), any());

        mockMvc.perform(delete("/api/posts/{id}", postId.toHexString()))
                .andExpect(status().isNoContent());

        verify(logicOfPosts).removePost(eq(postId), eq("alice@test.com"));
    }

    @Test
    @DisplayName("DELETE /api/posts/{id} → 404 when post not found")
    void deletePost_postNotFound_returns404() throws Exception {
        doThrow(new PostsNotFoundException("Post not found"))
                .when(logicOfPosts).removePost(any(), any());

        mockMvc.perform(delete("/api/posts/{id}", postId.toHexString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Post not found"));
    }

    @Test
    @DisplayName("DELETE /api/posts/{id} → 500 when unexpected error occurs")
    void deletePost_unexpectedError_returns500() throws Exception {
        doThrow(new RuntimeException("DB error")).when(logicOfPosts).removePost(any(), any());

        mockMvc.perform(delete("/api/posts/{id}", postId.toHexString()))
                .andExpect(status().isInternalServerError());
    }

    // -----------------------------------------------------------------------
    // PUT /api/posts/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("PUT /api/posts/{id} → 200 OK with updated post")
    void updatePost_returns200() throws Exception {
        post.setTitle("Updated Title");
        when(logicOfPosts.updatePost(eq(postId), any(PostDto.class), eq("alice@test.com")))
                .thenReturn(post);

        PostDto dto = new PostDto(null, "Updated Title", "Updated content", TypeOfAccess.PUBLIC);
        mockMvc.perform(put("/api/posts/{id}", postId.toHexString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    @DisplayName("PUT /api/posts/{id} → 404 when post not found")
    void updatePost_postNotFound_returns404() throws Exception {
        when(logicOfPosts.updatePost(any(), any(), any()))
                .thenThrow(new PostsNotFoundException("Post is not found"));

        PostDto dto = new PostDto(null, "Title", "Content", TypeOfAccess.PUBLIC);
        mockMvc.perform(put("/api/posts/{id}", postId.toHexString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/posts/{id} → 400 when title is blank")
    void updatePost_blankTitle_returns400() throws Exception {
        PostDto invalid = new PostDto(null, "", "Content", TypeOfAccess.PUBLIC);
        mockMvc.perform(put("/api/posts/{id}", postId.toHexString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/posts/{id} → service called with authenticated email")
    void updatePost_usesAuthenticatedEmail() throws Exception {
        when(logicOfPosts.updatePost(any(), any(), eq("alice@test.com"))).thenReturn(post);

        PostDto dto = new PostDto(null, "Title", "Content", TypeOfAccess.PUBLIC);
        mockMvc.perform(put("/api/posts/{id}", postId.toHexString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(logicOfPosts).updatePost(eq(postId), any(PostDto.class), eq("alice@test.com"));
    }

    // -----------------------------------------------------------------------
    // GET /api/posts/{postId}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/posts/{postId} → 200 OK with post detail")
    void getPostDetail_returns200() throws Exception {
        PostResponseDto postDto = new PostResponseDto(
                postId.toHexString(), "My Post", "Content",
                "alice@test.com", LocalDateTime.now(), 2L, 3L);
        PostDetailDto detail = new PostDetailDto(postDto, List.of(), List.of());

        when(logicOfPosts.getPostDetail(eq(postId), eq("alice@test.com"))).thenReturn(detail);

        mockMvc.perform(get("/api/posts/{postId}", postId.toHexString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Post details fetched successfully"))
                .andExpect(jsonPath("$.data.post.title").value("My Post"))
                .andExpect(jsonPath("$.data.post.commentCount").value(2))
                .andExpect(jsonPath("$.data.post.reactionCount").value(3));
    }

    @Test
    @DisplayName("GET /api/posts/{postId} → 404 when post not found")
    void getPostDetail_postNotFound_returns404() throws Exception {
        when(logicOfPosts.getPostDetail(any(), any()))
                .thenThrow(new PostsNotFoundException("Post not found"));

        mockMvc.perform(get("/api/posts/{postId}", postId.toHexString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Post not found"));
    }

    @Test
    @DisplayName("GET /api/posts/{postId} → 500 when access denied to private post")
    void getPostDetail_accessDenied_returns500() throws Exception {
        when(logicOfPosts.getPostDetail(any(), any()))
                .thenThrow(new AccessDeniedException("Cannot view this post"));

        mockMvc.perform(get("/api/posts/{postId}", postId.toHexString()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /api/posts/{postId} → uses authenticated user's email")
    void getPostDetail_usesAuthenticatedEmail() throws Exception {
        PostDetailDto detail = new PostDetailDto(
                new PostResponseDto(), List.of(), List.of());
        when(logicOfPosts.getPostDetail(eq(postId), eq("alice@test.com"))).thenReturn(detail);

        mockMvc.perform(get("/api/posts/{postId}", postId.toHexString()))
                .andExpect(status().isOk());

        verify(logicOfPosts).getPostDetail(eq(postId), eq("alice@test.com"));
    }
}
