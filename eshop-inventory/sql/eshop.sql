/*
SQLyog Ultimate v11.25 (64 bit)
MySQL - 5.5.54 : Database - eshop
*********************************************************************
*/


/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
USE `eshop`;

/*Table structure for table `brand` */

CREATE TABLE `brand` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;

/*Data for the table `brand` */

insert  into `brand`(`id`,`name`,`description`) values (1,'三星','韩国品牌22');
insert  into `brand`(`id`,`name`,`description`) values (2,'iPhone','美国品牌99');
insert  into `brand`(`id`,`name`,`description`) values (3,'测试品牌1111','测试品牌1111');
insert  into `brand`(`id`,`name`,`description`) values (4,'测试品牌2','测试品牌2');

/*Table structure for table `category` */

CREATE TABLE `category` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;

/*Data for the table `category` */

insert  into `category`(`id`,`name`,`description`) values (1,'平板电脑','电子产品');
insert  into `category`(`id`,`name`,`description`) values (2,'手机','电子类');
insert  into `category`(`id`,`name`,`description`) values (4,'汽车','出行类');
insert  into `category`(`id`,`name`,`description`) values (5,'笔记本电脑','电子产品');
insert  into `category`(`id`,`name`,`description`) values (6,'iPhone','热门手机分类');

/*Table structure for table `product` */

CREATE TABLE `product` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `category_id` int(11) DEFAULT NULL,
  `brand_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

/*Data for the table `product` */

insert  into `product`(`id`,`name`,`category_id`,`brand_id`) values (1,'Apple/苹果 iPhone 7 中国红亮黑色磨砂黑 国行全网通 港版未激活222',6,2);

/*Table structure for table `product_intro` */

CREATE TABLE `product_intro` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `content` varchar(255) DEFAULT NULL,
  `product_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

/*Data for the table `product_intro` */

insert  into `product_intro`(`id`,`content`,`product_id`) values (1,'1.jpg,2.jpg,3.jpg,4.jpg,5.jpg,6.jpg,7.png',1);

/*Table structure for table `product_inventory` */

CREATE TABLE `product_inventory` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `inventory_cnt` int(11) DEFAULT NULL,
  `product_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

/*Data for the table `product_inventory` */

insert  into `product_inventory`(`id`,`inventory_cnt`,`product_id`) values (1,755,1);

/*Table structure for table `product_price` */

CREATE TABLE `product_price` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `value` decimal(11,2) DEFAULT NULL,
  `product_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

/*Data for the table `product_price` */

insert  into `product_price`(`id`,`value`,`product_id`) values (1,'7480.00',1);

/*Table structure for table `product_property` */

CREATE TABLE `product_property` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `value` varchar(255) DEFAULT NULL,
  `product_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

/*Data for the table `product_property` */

insert  into `product_property`(`id`,`name`,`value`,`product_id`) values (1,'ihpone手机的属性','\"机身颜色=金色,银色;存储容量=32GB,128GB;版本类型=港澳台,中国大陆\"',1);

/*Table structure for table `product_specification` */

CREATE TABLE `product_specification` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `value` varchar(255) DEFAULT NULL,
  `product_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

/*Data for the table `product_specification` */

insert  into `product_specification`(`id`,`name`,`value`,`product_id`) values (1,'iphone手机的规格','上市时间=2017-03,机身厚度=0.7cm,售后保障=五星服务',1);

/*Table structure for table `user` */

CREATE TABLE `user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `age` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*Data for the table `user` */

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
