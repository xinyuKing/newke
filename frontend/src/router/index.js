import { createRouter, createWebHistory } from "vue-router";

import Home from "../views/Home.vue";
import PostDetail from "../views/PostDetail.vue";
import Login from "../views/Login.vue";
import Register from "../views/Register.vue";
import Settings from "../views/Settings.vue";
import Profile from "../views/Profile.vue";
import Search from "../views/Search.vue";
import Messages from "../views/Messages.vue";
import MessageDetail from "../views/MessageDetail.vue";
import Notices from "../views/Notices.vue";
import NoticeDetail from "../views/NoticeDetail.vue";
import Followees from "../views/Followees.vue";
import Followers from "../views/Followers.vue";
import MyPosts from "../views/MyPosts.vue";
import MyReplies from "../views/MyReplies.vue";
import AdminData from "../views/AdminData.vue";
import NotFound from "../views/NotFound.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", component: Home },
    { path: "/post/:id", component: PostDetail },
    { path: "/login", component: Login },
    { path: "/register", component: Register },
    { path: "/settings", component: Settings },
    { path: "/profile/:id", component: Profile },
    { path: "/search", component: Search },
    { path: "/messages", component: Messages },
    { path: "/messages/:conversationId", component: MessageDetail },
    { path: "/notices", component: Notices },
    { path: "/notices/:topic", component: NoticeDetail },
    { path: "/followees/:id", component: Followees },
    { path: "/followers/:id", component: Followers },
    { path: "/myposts/:id", component: MyPosts },
    { path: "/myreplies/:id", component: MyReplies },
    { path: "/admin/data", component: AdminData },
    { path: "/:pathMatch(.*)*", component: NotFound }
  ]
});

export default router;
