FROM nginx
COPY configs/http-dynamic-upstream.conf /etc/nginx/modules-enabled
COPY configs/alb /etc/nginx/sites-available
RUN ln -s /etc/nginx/sites-available/alb /etc/nginx/sites-enabled/alb
